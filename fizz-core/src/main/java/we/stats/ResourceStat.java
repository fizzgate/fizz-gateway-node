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

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.stats.circuitbreaker.CircuitBreaker;

/**
 * 
 * @author Francis Dong
 *
 */
public class ResourceStat {

	private static final Logger log = LoggerFactory.getLogger(ResourceStat.class);

	/**
	 * Resource ID
	 */
	private String resourceId;

	/**
	 * Request count of time slot, the beginning timestamp(timeId) as key
	 */
	private ConcurrentMap<Long, TimeSlot> timeSlots = new ConcurrentHashMap<>(256);

	/**
	 * Concurrent requests
	 */
	private AtomicLong concurrentRequests = new AtomicLong(0);

	private ReentrantReadWriteLock rwl1 = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock rwl2 = new ReentrantReadWriteLock();
	private Lock w1 = rwl1.writeLock();
	private Lock w2 = rwl2.writeLock();

	public ResourceStat(String resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * Returns Time slot of the specified time slot ID
	 * 
	 * @param timeSlotId
	 * @return
	 */
	public TimeSlot getTimeSlot(long timeSlotId) {
		if (timeSlots.containsKey(timeSlotId)) {
			return timeSlots.get(timeSlotId);
		} else {
			TimeSlot timeSlot = new TimeSlot(timeSlotId);
			timeSlot.setPeakConcurrentRequests(this.concurrentRequests.get());
			TimeSlot old = timeSlots.putIfAbsent(timeSlotId, timeSlot);
			if (old != null) {
				return old;
			} else {
				return timeSlot;
			}
		}
	}

	/**
	 * Increase concurrent request counter of the resource
	 * 
	 * @param timeSlotId
	 * @param maxCon
	 * @return false if exceed the maximum concurrent request of the specified
	 *         resource
	 */
	public boolean incrConcurrentRequest(long timeSlotId, Long maxCon) {
		w1.lock();
		try {
			boolean isExceeded = false;
			if (maxCon != null && maxCon.intValue() > 0) {
				long n = this.concurrentRequests.get();
				if (n >= maxCon.longValue()) {
					isExceeded = true;
					this.incrBlockRequestToTimeSlot(timeSlotId);
				} else {
					long conns = this.concurrentRequests.incrementAndGet();
					this.getTimeSlot(timeSlotId).updatePeakConcurrentReqeusts(conns);
				}
			} else {
				long conns = this.concurrentRequests.incrementAndGet();
				this.getTimeSlot(timeSlotId).updatePeakConcurrentReqeusts(conns);
			}
			return !isExceeded;
		} finally {
			w1.unlock();
		}
	}

	/**
	 * Decrease concurrent request counter of the resource
	 * 
	 */
	public void decrConcurrentRequest(long timeSlotId) {
		long conns = this.concurrentRequests.decrementAndGet();
		this.getTimeSlot(timeSlotId).updatePeakConcurrentReqeusts(conns);
	}

	/**
	 * Increase block request to the specified time slot
	 * 
	 */
	public void incrBlockRequestToTimeSlot(long timeSlotId) {
//		this.getTimeSlot(timeSlotId).getBlockRequests().incrementAndGet();
		this.getTimeSlot(timeSlotId).incrBlockRequests();
	}
	
	/**
	 * Increase total block request to the specified time slot
	 * 
	 */
	public void incrTotalBlockRequestToTimeSlot(long timeSlotId) {
//		this.getTimeSlot(timeSlotId).getTotalBlockRequests().incrementAndGet();
		this.getTimeSlot(timeSlotId).incrTotalBlockRequests();
	}

	/**
	 * Add request to the specified time slot
	 * 
	 * @param timeSlotId
	 * @return false if exceed the maximum RPS of the specified resource
	 */
	public boolean incrRequestToTimeSlot(long timeSlotId, Long maxRPS) {
		w2.lock();
		try {
			boolean isExceeded = false;
			if (maxRPS != null && maxRPS.intValue() > 0) {
//				TimeWindowStat timeWindowStat = this.getCurrentTimeWindowStat(resourceId, curTimeSlotId);
//				if (new BigDecimal(maxRPS).compareTo(timeWindowStat.getRps()) <= 0) {
//					isExceeded = true;
//					resourceStat.incrBlockRequestToTimeSlot(curTimeSlotId);
//				}

				// time slot unit is one second
				long total = this.getTimeSlot(timeSlotId).getCounter();
				long max = Long.valueOf(maxRPS);
				if (total >= max) {
					isExceeded = true;
					this.incrBlockRequestToTimeSlot(timeSlotId);
					this.decrConcurrentRequest(timeSlotId);
				} else {
					this.getTimeSlot(timeSlotId).incr();
				}
			} else {
				this.getTimeSlot(timeSlotId).incr();
			}
			return !isExceeded;
		} finally {
			w2.unlock();
		}
	}

	public void updateCircuitBreakState(long timeSlot, CircuitBreaker.State current, CircuitBreaker.State target) {
		getTimeSlot(timeSlot).getCircuitBreakState().compareAndSet(current, target);
	}

	public void incrCircuitBreakNum(long timeSlot) {
		getTimeSlot(timeSlot).incrCircuitBreakNum();
	}

//	public void decrCircuitBreakNum(long timeSlot) {
//		getTimeSlot(timeSlot).getCircuitBreakNum().decrementAndGet();
//	}
//
//	public void incrGradualResumeNum(long timeSlot) {
//		getTimeSlot(timeSlot).getGradualResumeNum().incrementAndGet();
//	}
//
//	public void decrGradualResumeNum(long timeSlot) {
//		getTimeSlot(timeSlot).getGradualResumeNum().decrementAndGet();
//	}
//
//	public void incrGradualRejectNum(long timeSlot) {
//		getTimeSlot(timeSlot).getGradualRejectNum().incrementAndGet();
//	}
//
//	public void decrGradualRejectNum(long timeSlot) {
//		getTimeSlot(timeSlot).getGradualRejectNum().decrementAndGet();
//	}

	public void incr2xxStatusCount(long timeSlot) {
		getTimeSlot(timeSlot).get2xxStatusCount().incrementAndGet();
	}

	public void incr4xxStatusCount(long timeSlot) {
		getTimeSlot(timeSlot).get4xxStatusCount().incrementAndGet();
	}

	public void incr5xxStatusCount(long timeSlot) {
		getTimeSlot(timeSlot).get5xxStatusCount().incrementAndGet();
	}

	public void incr504StatusCount(long timeSlot) {
		getTimeSlot(timeSlot).get504StatusCount().incrementAndGet();
	}

	/**
	 * Add request RT to the specified time slot
	 * 
	 * @param timeSlotId
	 * @param rt         response time of the request
	 * @param isSuccess  Whether the request is success or not
	 * @return
	 */
	public void addRequestRT(long timeSlotId, long rt, boolean isSuccess) {
		this.getTimeSlot(timeSlotId).addRequestRT(rt, isSuccess);
	}

	/**
	 * Returns statistic of the specified time window
	 * 
	 * @param startSlotId
	 * @param endSlotId
	 * @return
	 */
	public TimeWindowStat getTimeWindowStat(long startSlotId, long endSlotId) {
		TimeWindowStat tws = new TimeWindowStat();

		tws.setStartTime(startSlotId);
		tws.setEndTime(endSlotId);

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		long totalReqs = 0;
		long totalRt = 0;
		long peakConcurrences = 0;
		long peakRps = 0;
		long errors = 0;
		long blockReqs = 0;
		long totalBlockReqs = 0;
		long compReqs = 0;

		int _2xxStatus = 0;
		int _4xxStatus = 0;
		int _5xxStatus = 0;
		int _504Status = 0;

		for (long i = startSlotId; i < endSlotId;) {
			if (timeSlots.containsKey(i)) {
				TimeSlot timeSlot = timeSlots.get(i);
				min = timeSlot.getMin() < min ? timeSlot.getMin() : min;
				max = timeSlot.getMax() > max ? timeSlot.getMax() : max;
				peakConcurrences = timeSlot.getPeakConcurrentRequests() > peakConcurrences
						? timeSlot.getPeakConcurrentRequests()
						: peakConcurrences;
				peakRps = timeSlot.getCounter() > peakRps ? timeSlot.getCounter() : peakRps;
				totalReqs = totalReqs + timeSlot.getCounter();
				totalRt = totalRt + timeSlot.getTotalRt();
				errors = errors + timeSlot.getErrors();
				blockReqs = blockReqs + timeSlot.getBlockRequests();
				totalBlockReqs = totalBlockReqs + timeSlot.getTotalBlockRequests();
				compReqs = compReqs + timeSlot.getCompReqs();

				_2xxStatus = _2xxStatus + timeSlot.get2xxStatusCount().get();
				_4xxStatus = _4xxStatus + timeSlot.get4xxStatusCount().get();
				_5xxStatus = _5xxStatus + timeSlot.get5xxStatusCount().get();
				_504Status = _504Status + timeSlot.get504StatusCount().get();
			}
			i = i + FlowStat.INTERVAL;
		}
		tws.setMin(min == Long.MAX_VALUE ? null : min);
		tws.setMax(max == Long.MIN_VALUE ? null : max);
		tws.setPeakConcurrentReqeusts(peakConcurrences);
		tws.setTotal(totalReqs);
		tws.setErrors(errors);
		tws.setBlockRequests(blockReqs);
		tws.setTotalBlockRequests(totalBlockReqs);
		tws.setCompReqs(compReqs);
		tws.setPeakRps(new BigDecimal(peakRps));

		tws.set2xxStatus(_2xxStatus);
		tws.set4xxStatus(_4xxStatus);
		tws.set5xxStatus(_5xxStatus);
		tws.set504Status(_504Status);

		if (compReqs > 0) {
			tws.setAvgRt(totalRt / compReqs);
		}
		
		if (totalReqs > 0) {
			BigDecimal nsec = new BigDecimal(endSlotId - startSlotId).divide(new BigDecimal(1000), 5,
					BigDecimal.ROUND_HALF_UP);
			BigDecimal rps = new BigDecimal(totalReqs).divide(nsec, 5, BigDecimal.ROUND_HALF_UP);

			if (rps.compareTo(new BigDecimal(10)) >= 0) {
				rps = rps.setScale(0, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
			} else {
				rps = rps.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
			}
			tws.setRps(rps);
		}

		return tws;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public ConcurrentMap<Long, TimeSlot> getTimeSlots() {
		return timeSlots;
	}

	public void setTimeSlots(ConcurrentMap<Long, TimeSlot> timeSlots) {
		this.timeSlots = timeSlots;
	}

	public AtomicLong getConcurrentRequests() {
		return concurrentRequests;
	}

	public void setConcurrentRequests(AtomicLong concurrentRequests) {
		this.concurrentRequests = concurrentRequests;
	}
}
