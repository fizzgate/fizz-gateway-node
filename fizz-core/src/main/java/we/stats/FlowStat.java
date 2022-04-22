/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.stats;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import we.stats.circuitbreaker.CircuitBreakManager;
import we.stats.circuitbreaker.CircuitBreaker;
import we.util.ResourceIdUtils;
import we.util.WebUtils;

/**
 * Flow Statistic
 * 
 * @author Francis Dong
 *
 */
public class FlowStat {

	private static final Logger log = LoggerFactory.getLogger(FlowStat.class);

	/**
	 * Time slot interval in millisecond
	 */
	public static long INTERVAL = 1000;

	/**
	 * A string Resource ID as key
	 */
	public ConcurrentMap<String, ResourceStat> resourceStats = new ConcurrentHashMap<>(256);

	/**
	 * Retention time of statistic data
	 */
	public static long RETENTION_TIME_IN_MINUTES = 5;

	private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private Lock w = rwl.writeLock();

	private ExecutorService pool = Executors.newFixedThreadPool(2);

	private CircuitBreakManager circuitBreakManager;

	public FlowStat() {
		runScheduleJob();
	}

	public FlowStat(CircuitBreakManager circuitBreakManager) {
		this.circuitBreakManager = circuitBreakManager;
		runScheduleJob();
	}

	public void setCircuitBreakManager(CircuitBreakManager circuitBreakManager) {
		this.circuitBreakManager = circuitBreakManager;
	}

	private void runScheduleJob() {
		pool.submit(new HousekeepJob(this));
		pool.submit(new PeakConcurrentJob(this));
	}

	/**
	 * Update retention time
	 * 
	 * @param retentionTimeInMinutes
	 */
	public void updateRetentionTime(int retentionTimeInMinutes) {
		RETENTION_TIME_IN_MINUTES = retentionTimeInMinutes;
	}

	/**
	 * Returns the current time slot ID
	 * 
	 * @return
	 */
	public long currentTimeSlotId() {
		return (System.currentTimeMillis() / INTERVAL) * INTERVAL;
	}

	/**
	 * Returns the time slot ID of the specified time
	 * 
	 * @param timeMilli
	 * @return
	 */
	public long getTimeSlotId(long timeMilli) {
		return (System.currentTimeMillis() / INTERVAL) * INTERVAL;
	}
	
	/**
	 * Increase concurrent request counter for given resources chain
	 * 
	 * @param resourceConfigs Resource configurations
	 * @param curTimeSlotId   current time slot ID, it should be generated by
	 *                        Flowstat.currentTimeSlotId()
	 * @return IncrRequestResult
	 */
	public IncrRequestResult incrRequest(List<ResourceConfig> resourceConfigs, long curTimeSlotId) {
		return incrRequest(resourceConfigs, curTimeSlotId, null);
	}
	
	/**
	 * Increase concurrent request counter for given resources chain
	 * 
	 * @param resourceConfigs Resource configurations
	 * @param curTimeSlotId   current time slot ID, it should be generated by
	 *                        Flowstat.currentTimeSlotId()
	 * @param totalBlockFunc  [optional] callback function for statistic of total
	 *                        block requests of the resource and its parent resources
	 * @return IncrRequestResult
	 */
	public IncrRequestResult incrRequest(List<ResourceConfig> resourceConfigs, long curTimeSlotId,
			BiFunction<ResourceConfig, List<ResourceConfig>, List<ResourceConfig>> totalBlockFunc) {
		if (resourceConfigs == null || resourceConfigs.size() == 0) {
			return null;
		}
		w.lock();
		try {
			// check if exceed limit
			for (ResourceConfig resourceConfig : resourceConfigs) {
				long maxCon = resourceConfig.getMaxCon();
				long maxQPS = resourceConfig.getMaxQPS();
				if (maxCon > 0 || maxQPS > 0) {
					ResourceStat resourceStat = getResourceStat(resourceConfig.getResourceId());
					// check concurrent request
					if (maxCon > 0) {
						long n = resourceStat.getConcurrentRequests().get();
						if (n >= maxCon) {
							resourceStat.incrBlockRequestToTimeSlot(curTimeSlotId);
							if (totalBlockFunc != null) {
								List<ResourceConfig> parentResCfgs = totalBlockFunc.apply(resourceConfig,
										resourceConfigs);
								if (parentResCfgs != null && parentResCfgs.size() > 0) {
									for (ResourceConfig pResCfg : parentResCfgs) {
										getResourceStat(pResCfg.getResourceId())
												.incrTotalBlockRequestToTimeSlot(curTimeSlotId);
									}
								}
							}
							return IncrRequestResult.block(resourceConfig.getResourceId(),
									BlockType.CONCURRENT_REQUEST);
						}
					}

					// check QPS
					if (maxQPS > 0) {
						long total = resourceStat.getTimeSlot(curTimeSlotId).getCounter();
						if (total >= maxQPS) {
							resourceStat.incrBlockRequestToTimeSlot(curTimeSlotId);
							if (totalBlockFunc != null) {
								List<ResourceConfig> parentResCfgs = totalBlockFunc.apply(resourceConfig,
										resourceConfigs);
								if (parentResCfgs != null && parentResCfgs.size() > 0) {
									for (ResourceConfig pResCfg : parentResCfgs) {
										getResourceStat(pResCfg.getResourceId())
												.incrTotalBlockRequestToTimeSlot(curTimeSlotId);
									}
								}
							}
							return IncrRequestResult.block(resourceConfig.getResourceId(), BlockType.QPS);
						}
					}
				}
			}

			// increase request and concurrent request
			for (ResourceConfig resourceConfig : resourceConfigs) {
				ResourceStat resourceStat = getResourceStat(resourceConfig.getResourceId());
				int cons = resourceStat.getConcurrentRequests().incrementAndGet();
				resourceStat.getTimeSlot(curTimeSlotId).updatePeakConcurrentReqeusts(cons);
				resourceStat.getTimeSlot(curTimeSlotId).incr();
			}
			return IncrRequestResult.success();
		} finally {
			w.unlock();
		}
	}

	public IncrRequestResult incrRequest(ServerWebExchange exchange, List<ResourceConfig> resourceConfigs, long curTimeSlotId,
										 BiFunction<ResourceConfig, List<ResourceConfig>, List<ResourceConfig>> totalBlockFunc) {
		if (resourceConfigs == null || resourceConfigs.size() == 0) {
			return null;
		}
		w.lock();
		try {
			// check if exceed limit
			for (ResourceConfig resourceConfig : resourceConfigs) {
				long maxCon = resourceConfig.getMaxCon();
				long maxQPS = resourceConfig.getMaxQPS();
				if (maxCon > 0 || maxQPS > 0) {
					ResourceStat resourceStat = getResourceStat(resourceConfig.getResourceId());
					// check concurrent request
					if (maxCon > 0) {
						long n = resourceStat.getConcurrentRequests().get();
						if (n >= maxCon) {
							resourceStat.incrBlockRequestToTimeSlot(curTimeSlotId);
							if (totalBlockFunc != null) {
								List<ResourceConfig> parentResCfgs = totalBlockFunc.apply(resourceConfig,
										resourceConfigs);
								if (parentResCfgs != null && parentResCfgs.size() > 0) {
									for (ResourceConfig pResCfg : parentResCfgs) {
										getResourceStat(pResCfg.getResourceId())
												.incrTotalBlockRequestToTimeSlot(curTimeSlotId);
									}
								}
							}
							return IncrRequestResult.block(resourceConfig.getResourceId(),
									BlockType.CONCURRENT_REQUEST);
						}
					}

					// check QPS
					if (maxQPS > 0) {
						long total = resourceStat.getTimeSlot(curTimeSlotId).getCounter();
						if (total >= maxQPS) {
							resourceStat.incrBlockRequestToTimeSlot(curTimeSlotId);
							if (totalBlockFunc != null) {
								List<ResourceConfig> parentResCfgs = totalBlockFunc.apply(resourceConfig,
										resourceConfigs);
								if (parentResCfgs != null && parentResCfgs.size() > 0) {
									for (ResourceConfig pResCfg : parentResCfgs) {
										getResourceStat(pResCfg.getResourceId())
												.incrTotalBlockRequestToTimeSlot(curTimeSlotId);
									}
								}
							}
							return IncrRequestResult.block(resourceConfig.getResourceId(), BlockType.QPS);
						}
					}
				}
			}

			for (ResourceConfig resourceConfig : resourceConfigs) {
				getResourceStat(resourceConfig.getResourceId());
			}

			String service = WebUtils.getClientService(exchange);
			String path    = WebUtils.getClientReqPath(exchange);
			String resource = ResourceIdUtils.buildResourceId(null, null, null, service, path);
			boolean permit = circuitBreakManager.permit(exchange, curTimeSlotId, this, service, path);
			if (!permit) {
				return IncrRequestResult.block(resource, BlockType.CIRCUIT_BREAK);
			}

			// increase request and concurrent request
			for (ResourceConfig resourceConfig : resourceConfigs) {
				ResourceStat resourceStat = getResourceStat(resourceConfig.getResourceId());
				int cons = resourceStat.getConcurrentRequests().incrementAndGet();
				resourceStat.getTimeSlot(curTimeSlotId).updatePeakConcurrentReqeusts(cons);
				resourceStat.getTimeSlot(curTimeSlotId).incr();
			}
			return IncrRequestResult.success();
		} finally {
			w.unlock();
		}
	}

	/**
	 * Add request RT and Decrease concurrent request for given resources chain
	 * 
	 * @param resourceConfigs
	 * @param timeSlotId
	 * @param rt
	 * @param isSuccess
	 */
	public void addRequestRT(List<ResourceConfig> resourceConfigs, long timeSlotId, long rt, boolean isSuccess, HttpStatus statusCode) {
		if (resourceConfigs == null || resourceConfigs.size() == 0) {
			return;
		}
		for (int i = resourceConfigs.size() - 1; i >= 0; i--) {
			ResourceStat resourceStat = getResourceStat(resourceConfigs.get(i).getResourceId());
			resourceStat.decrConcurrentRequest(timeSlotId);
			resourceStat.addRequestRT(timeSlotId, rt, isSuccess);

			if (statusCode.is2xxSuccessful()) {
				resourceStat.incr2xxStatusCount(timeSlotId);

			} else if (statusCode.is4xxClientError()) {
				resourceStat.incr4xxStatusCount(timeSlotId);

			} else if (statusCode.is5xxServerError()) {
				resourceStat.incr5xxStatusCount(timeSlotId);

			} else if (statusCode == HttpStatus.GATEWAY_TIMEOUT) {
				resourceStat.incr504StatusCount(timeSlotId);
			}
		}
	}

	/*public void addRequestRT(ServerWebExchange exchange, List<ResourceConfig> resourceConfigs, long timeSlotId, long rt, boolean isSuccess) {
		if (resourceConfigs == null || resourceConfigs.size() == 0) {
			return;
		}
		for (int i = resourceConfigs.size() - 1; i >= 0; i--) {
			ResourceStat resourceStat = getResourceStat(resourceConfigs.get(i).getResourceId());
			resourceStat.decrConcurrentRequest(timeSlotId);
			resourceStat.addRequestRT(timeSlotId, rt, isSuccess);
		}

		String service = WebUtils.getClientService(exchange);
		String path    = WebUtils.getClientReqPath(exchange);
		circuitBreakManager.correctCircuitBreakerStateAsError(exchange, timeSlotId, this, service, path);
	}*/

	/**
	 * Increase concurrent request counter of the specified resource
	 * 
	 * @param resourceId    Resource ID
	 * @param curTimeSlotId current time slot ID, it should be generated by
	 *                      Flowstat.currentTimeSlotId()
	 * @param maxCon        Maximum concurrent request of the specified resource,
	 *                      null/zero/negative for no limit
	 * @param maxRPS        Maximum RPS of the specified resource,
	 *                      null/zero/negative for no limit
	 * @return true if the request is not blocked; false if exceed the maximum
	 *         concurrent request/RPS of the specified resource
	 */
	public boolean incrRequest(String resourceId, long curTimeSlotId, Long maxCon, Long maxRPS) {
		ResourceStat resourceStat = getResourceStat(resourceId);
		boolean success = resourceStat.incrConcurrentRequest(curTimeSlotId, maxCon);
		if (success) {
			success = resourceStat.incrRequestToTimeSlot(curTimeSlotId, maxRPS);
		}
		return success;
	}

	/**
	 * Decrease concurrent request of the specified resource of the specified time
	 * slot
	 * 
	 * @param resourceId Resource ID
	 * @param timeSlotId TimeSlot ID
	 * @return
	 */
	public void decrConcurrentRequest(String resourceId, long timeSlotId) {
		if (resourceId == null) {
			return;
		}
		ResourceStat resourceStat = getResourceStat(resourceId);

		long conns = resourceStat.getConcurrentRequests().get();
		resourceStat.decrConcurrentRequest(timeSlotId);
	}

	/**
	 * Add request RT to the specified time slot counter
	 * 
	 * @param resourceId Resource ID
	 * @param timeSlotId TimeSlot ID
	 * @param rt         Response time of request
	 * @param isSuccess  Whether the request is success or not
	 * @return
	 */
	public void addRequestRT(String resourceId, long timeSlotId, long rt, boolean isSuccess) {
		if (resourceId == null) {
			return;
		}
		ResourceStat resourceStat = getResourceStat(resourceId);
		resourceStat.addRequestRT(timeSlotId, rt, isSuccess);
	}

	public ResourceStat getResourceStat(String resourceId) {
		ResourceStat resourceStat = null;
		if (resourceStats.containsKey(resourceId)) {
			resourceStat = resourceStats.get(resourceId);
		} else {
			resourceStat = new ResourceStat(resourceId);
			ResourceStat rs = resourceStats.putIfAbsent(resourceId, resourceStat);
			if (rs != null) {
				resourceStat = rs;
			}
		}
		return resourceStat;
	}

	/**
	 * Returns the current concurrent requests of the specified resource<br/>
	 * <br/>
	 * 
	 * @param resourceId Resource ID
	 */
	public long getConcurrentRequests(String resourceId) {
		ResourceStat resourceStat = getResourceStat(resourceId);
		return resourceStat.getConcurrentRequests().get();
	}

	/**
	 * Returns current TimeWindowStat of the specified resource
	 * 
	 * @param resourceId
	 * @return
	 */
	public TimeWindowStat getCurrentTimeWindowStat(String resourceId) {
		long startTimeMilli = currentTimeSlotId();
		return getTimeWindowStat(resourceId, startTimeMilli, startTimeMilli + 1000);
	}

	/**
	 * Returns current TimeWindowStat of the specified resource
	 * 
	 * @param resourceId
	 * @param curTimeSlotId
	 * @return
	 */
	@SuppressWarnings("unused")
	private TimeWindowStat getCurrentTimeWindowStat(String resourceId, long curTimeSlotId) {
		return getTimeWindowStat(resourceId, curTimeSlotId, curTimeSlotId + 1000);
	}

	/**
	 * Returns the TimeWindowStat of previous second of the specified time
	 * 
	 * @param resourceId
	 * @param timeMilli
	 * @return
	 */
	public TimeWindowStat getPreviousSecondStat(String resourceId, long timeMilli) {
		long endTimeMilli = (timeMilli / INTERVAL) * INTERVAL;
		return getTimeWindowStat(resourceId, endTimeMilli - 1000, endTimeMilli);
	}

	/**
	 * Returns the timeWindowStat of the specific resource in the specified time
	 * window [startTimeMilli, endTimeMilli)
	 * 
	 * @param startTimeMilli included
	 * @param endTimeMilli   excluded
	 * @return
	 */
	public TimeWindowStat getTimeWindowStat(String resourceId, long startTimeMilli, long endTimeMilli) {
		long startSlotId = (startTimeMilli / INTERVAL) * INTERVAL;
		long endSlotId = (endTimeMilli / INTERVAL) * INTERVAL;

		if (startSlotId == endSlotId) {
			endSlotId = endSlotId + INTERVAL;
		}
		if (resourceStats.containsKey(resourceId)) {
			ResourceStat resourceStat = resourceStats.get(resourceId);
			return resourceStat.getTimeWindowStat(startSlotId, endSlotId);
		}
		return null;
	}

	/**
	 * Returns the ResourceTimeWindowStat list in the specified time window
	 * [startTimeMilli, endTimeMilli), The time slot unit is one second
	 * 
	 * @param resourceId     optional, returns ResourceSlot list of all resources
	 *                       while resourceId is null
	 * @param startTimeMilli
	 * @param endTimeMilli
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<ResourceTimeWindowStat> getResourceTimeWindowStats(String resourceId, long startTimeMilli,
			long endTimeMilli) {
		return this.getResourceTimeWindowStats(resourceId, startTimeMilli, endTimeMilli, 1);
	}

	/**
	 * Returns the ResourceTimeWindow list in the specified time window
	 * [startTimeMilli, endTimeMilli)
	 * 
	 * @param resourceId        optional, returns ResourceTimeWindowStat list of all
	 *                          resources while resourceId is null
	 * @param startTimeMilli
	 * @param endTimeMilli
	 * @param slotIntervalInSec interval of custom time slot in millisecond, such as
	 *                          60 for 1 minutes
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<ResourceTimeWindowStat> getResourceTimeWindowStats(String resourceId, long startTimeMilli,
			long endTimeMilli, long slotIntervalInSec) {
		List<ResourceTimeWindowStat> list = new ArrayList<>();
		long startSlotId = (startTimeMilli / INTERVAL) * INTERVAL;
		long endSlotId = (endTimeMilli / INTERVAL) * INTERVAL;

		if (startSlotId == endSlotId) {
			endSlotId = endSlotId + INTERVAL;
		}
		if (slotIntervalInSec < 1 || (endSlotId - startSlotId) / 1000 < slotIntervalInSec) {
			return list;
		}
		long slotInterval = slotIntervalInSec * 1000;

		if (resourceId == null) {
			Set<Map.Entry<String, ResourceStat>> entrys = resourceStats.entrySet();
			for (Entry<String, ResourceStat> entry : entrys) {
				String rid = entry.getKey();
				ResourceTimeWindowStat resourceWin = new ResourceTimeWindowStat(rid);
				long end = startSlotId + slotInterval;
				for (long start = startSlotId; end <= endSlotId;) {
					TimeWindowStat tws = getTimeWindowStat(rid, start, end);
					if (tws != null) {
						resourceWin.getWindows().add(tws);
					}
					start += slotInterval;
					end += slotInterval;
				}
				if (resourceWin.getWindows().size() > 0) {
					list.add(resourceWin);
				}
			}
		} else {
			ResourceTimeWindowStat resourceWin = new ResourceTimeWindowStat(resourceId);
			long end = startSlotId + slotInterval;
			for (long start = startSlotId; end <= endSlotId;) {
				TimeWindowStat tws = getTimeWindowStat(resourceId, start, end);
				if (tws != null) {
					resourceWin.getWindows().add(tws);
				}
				start += slotInterval;
				end += slotInterval;
			}
			if (resourceWin.getWindows().size() > 0) {
				list.add(resourceWin);
			}
		}
		return list;
	}

	class HousekeepJob implements Runnable {

		private FlowStat stat;

		public HousekeepJob(FlowStat stat) {
			this.stat = stat;
		}

		@Override
		public void run() {
			long n = FlowStat.RETENTION_TIME_IN_MINUTES * 60 * 1000 / FlowStat.INTERVAL * FlowStat.INTERVAL;
			long lastSlotId = stat.currentTimeSlotId() - n;
			while (true) {
				long slotId = stat.currentTimeSlotId() - n;
				/*if (log.isDebugEnabled()) {
					log.debug("{} - {} resource stats size {}", lastSlotId, slotId, stat.resourceStats.size());
				}
				Set<Map.Entry<String, ResourceStat>> es = stat.resourceStats.entrySet();
				for (Entry<String, ResourceStat> e : es) {
					String resourceId = e.getKey();
					ConcurrentMap<Long, TimeSlot> timeSlots = e.getValue().getTimeSlots();
					if (log.isDebugEnabled()) {
						log.debug("{} - {} {} has {} timeslot", lastSlotId, slotId, resourceId, timeSlots.size());
					}
				}*/
				for (long i = lastSlotId; i < slotId;) {
					Set<Map.Entry<String, ResourceStat>> entrys = stat.resourceStats.entrySet();
					for (Entry<String, ResourceStat> entry : entrys) {
						String resourceId = entry.getKey();
						ConcurrentMap<Long, TimeSlot> timeSlots = entry.getValue().getTimeSlots();
						/*if (log.isDebugEnabled()) {
							log.debug("{} - {} {} remove {} timeslot", lastSlotId, slotId, resourceId, i);
						}*/
						timeSlots.remove(i);
					}
					i = i + FlowStat.INTERVAL;
				}
				lastSlotId = slotId;
				// log.debug("housekeeping done");


				/*long currentTimeSlot = stat.currentTimeSlotId();
				long startTimeSlot = currentTimeSlot - n;
				for (Entry<String, ResourceStat> entry : stat.resourceStats.entrySet()) {
					String resource = entry.getKey();
					if (ResourceIdUtils.NODE_RESOURCE.equals(resource)) {
						continue;
					}
					ResourceStat resourceStat = entry.getValue();
					boolean noTraffic = true;
					long timeSlot = startTimeSlot;
					for ( ; timeSlot < currentTimeSlot; timeSlot += FlowStat.INTERVAL) {
						int reqCnt = resourceStat.getTimeSlot(timeSlot).getCounter();
						if (reqCnt > 0) {
							noTraffic = false;
							break;
						}
					}
					if (noTraffic) {
						stat.resourceStats.remove(resource);
					}
				}*/


				try {
					Thread.sleep(10 * 1000);
				} catch (Exception e) {
					log.error("HouseKeepJob error", e);
				}
			}
		}
	}

	class PeakConcurrentJob implements Runnable {

		private FlowStat stat;

		public PeakConcurrentJob(FlowStat stat) {
			this.stat = stat;
		}

		@Override
		public void run() {
			Long lastTimeSlotId = null;
			while (true) {
				long curTimeSlotId = stat.currentTimeSlotId();
				if (lastTimeSlotId == null || lastTimeSlotId.longValue() != curTimeSlotId) {
					// log.debug("PeakConcurrentJob start");
					Set<Map.Entry<String, ResourceStat>> entrys = stat.resourceStats.entrySet();
					for (Entry<String, ResourceStat> entry : entrys) {
						String resource = entry.getKey();
						// log.debug("PeakConcurrentJob: resourceId={} slotId=={}", resourceId,
						// curTimeSlotId);

						ResourceStat resourceStat = entry.getValue();
						if (resourceStat.getConcurrentRequests().get() > 0) {
							resourceStat.getTimeSlot(curTimeSlotId);
						}

						// String resource = resourceStat.getResourceId();
						CircuitBreaker cb = circuitBreakManager.getCircuitBreaker(resource);
						if (cb != null) {
							cb.correctState(curTimeSlotId, stat);
						}
					}
					lastTimeSlotId = curTimeSlotId;
					// log.debug("PeakConcurrentJob done");
				}
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					log.error("PeakConcurrentJob error", e);
				}
			}
		}
	}

}
