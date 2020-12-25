package we.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * Resource ID for all resources entry
	 */
	public static String ALL_RESOURCES = "_ALL_RESOURCES";

	/**
	 * A string Resource ID as key
	 */
	public ConcurrentMap<String, ResourceStat> resourceStats = new ConcurrentHashMap<>();

	private ExecutorService pool = Executors.newFixedThreadPool(1);

	public FlowStat() {
		runHousekeepJob();
	}

	private void runHousekeepJob() {
		pool.submit(new HousekeepJob(this));
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
	 * Increment concurrent request counter of the specified resource
	 * 
	 * @param resourceId Resource ID
	 */
	public void incrConcurrentRequest(String resourceId) {
		ResourceStat resourceStat = getResourceStat(resourceId);
		ResourceStat allResourceStat = getResourceStat(ALL_RESOURCES);
		resourceStat.incrConcurrentRequest();
		allResourceStat.incrConcurrentRequest();
	}

	/**
	 * Returns the current concurrent requests of the specified resource<br/>
	 * <br/>
	 * Returns the current concurrent connections of all resources:<br/>
	 * getConnection(FlowStat.ALL_RESOURCES)
	 * 
	 * @param resourceId Resource ID
	 */
	public int getConcurrentRequests(String resourceId) {
		ResourceStat resourceStat = getResourceStat(resourceId);
		return resourceStat.getConcurrentRequests().get();
	}

	/**
	 * Add request to current time slot and decrement concurrent connection counter
	 * 
	 * @param resourceId Resource ID
	 * @param rt         Response time of request
	 * @param isSuccess  Whether the request is success or not
	 */
	public void incrRequest(String resourceId, long rt, boolean isSuccess) {
		incrRequestToTimeSlot(resourceId, currentTimeSlotId(), rt, isSuccess);
	}

	/**
	 * Add request to the specified time slot and decrement concurrent connection
	 * counter
	 * 
	 * @param resourceId Resource ID
	 * @param timeSlotId TimeSlot ID
	 * @param rt         Response time of request
	 * @param isSuccess  Whether the request is success or not
	 * @return
	 */
	public void incrRequestToTimeSlot(String resourceId, long timeSlotId, long rt, boolean isSuccess) {
		if (resourceId == null) {
			return;
		}
		ResourceStat resourceStat = getResourceStat(resourceId);
		ResourceStat allResourceStat = getResourceStat(ALL_RESOURCES);
		resourceStat.incrRequestToTimeSlot(timeSlotId, rt, isSuccess);
		allResourceStat.incrRequestToTimeSlot(timeSlotId, rt, isSuccess);
	}

	private ResourceStat getResourceStat(String resourceId) {
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
	 * Returns the TimeWindowStat of previous second
	 * 
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
	 * @param endTimemilli   excluded
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
				for (long i = startSlotId; i < endSlotId;) {
					TimeWindowStat tws = getTimeWindowStat(resourceId, startSlotId, endSlotId);
					if (tws != null) {
						resourceWin.getWindows().add(tws);
					}
					i = i + slotInterval;
				}
				if (resourceWin.getWindows().size() > 0) {
					list.add(resourceWin);
				}
			}
		} else {
			ResourceTimeWindowStat resourceWin = new ResourceTimeWindowStat(resourceId);
			for (long i = startSlotId; i < endSlotId;) {
				TimeWindowStat tws = getTimeWindowStat(resourceId, startSlotId, endSlotId);
				if (tws != null) {
					resourceWin.getWindows().add(tws);
				}
				i = i + slotInterval;
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
			long n = 2 * 60 * 60 * 1000 / FlowStat.INTERVAL * FlowStat.INTERVAL;
			long lastSlotId = stat.currentTimeSlotId() - n;
			while (true) {
				log.debug("housekeeping start");
				long slotId = stat.currentTimeSlotId() - n;
				for (long i = lastSlotId; i < slotId;) {
					Set<Map.Entry<String, ResourceStat>> entrys = stat.resourceStats.entrySet();
					for (Entry<String, ResourceStat> entry : entrys) {
						String resourceId = entry.getKey();
						ConcurrentMap<Long, TimeSlot> timeSlots = entry.getValue().getTimeSlots();
						log.debug("housekeeping remove slot: resourceId={} slotId=={}", resourceId, i);
						timeSlots.remove(i);
					}
					i = i + FlowStat.INTERVAL;
				}
				lastSlotId = slotId;
				log.debug("housekeeping done");
				try {
					Thread.sleep(60 * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
