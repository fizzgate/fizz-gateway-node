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
	 * Route ID for all routes entry
	 */
	public static String ALL_TOUTES = "_ALL_ROUTES";

	/**
	 * A string Route ID as key
	 */
	public ConcurrentMap<String, RouteStat> routeStats = new ConcurrentHashMap<>();

	
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
	 * Increment concurrent request counter of the specified route
	 * 
	 * @param routeId Route ID
	 */
	public void incrConcurrentRequest(String routeId) {
		RouteStat routeStat = getRouteStat(routeId);
		RouteStat allRouteStat = getRouteStat(ALL_TOUTES);
		routeStat.incrConcurrentRequest();
		allRouteStat.incrConcurrentRequest();
	}

	/**
	 * Returns the current concurrent requests of the specified route<br/>
	 * <br/>
	 * Returns the current concurrent connections of all routes:<br/>
	 * getConnection(FlowStat.ALL_TOUTES)
	 * 
	 * @param routeId Route ID
	 */
	public int getConcurrentRequests(String routeId) {
		RouteStat routeStat = getRouteStat(routeId);
		return routeStat.getConcurrentRequests().get();
	}

	/**
	 * Add request to current time slot and decrement concurrent connection counter
	 * 
	 * @param routeId   Route ID
	 * @param rt        Response time of request
	 * @param isSuccess Whether the request is success or not
	 */
	public void incrRequest(String routeId, long rt, boolean isSuccess) {
		incrRequestToTimeSlot(routeId, currentTimeSlotId(), rt, isSuccess);
	}

	/**
	 * Add request to the specified time slot and decrement concurrent connection
	 * counter
	 * 
	 * @param routeId    Route ID
	 * @param timeSlotId TimeSlot ID
	 * @param rt         Response time of request
	 * @param isSuccess  Whether the request is success or not
	 * @return
	 */
	public void incrRequestToTimeSlot(String routeId, long timeSlotId, long rt, boolean isSuccess) {
		if (routeId == null) {
			return;
		}
		RouteStat routeStat = getRouteStat(routeId);
		RouteStat allRouteStat = getRouteStat(ALL_TOUTES);
		routeStat.incrRequestToTimeSlot(timeSlotId, rt, isSuccess);
		allRouteStat.incrRequestToTimeSlot(timeSlotId, rt, isSuccess);
	}

	private RouteStat getRouteStat(String routeId) {
		RouteStat routeStat = null;
		if (routeStats.containsKey(routeId)) {
			routeStat = routeStats.get(routeId);
		} else {
			routeStat = new RouteStat(routeId);
			RouteStat rs = routeStats.putIfAbsent(routeId, routeStat);
			if (rs != null) {
				routeStat = rs;
			}
		}
		return routeStat;
	}

	/**
	 * Returns current TimeWindowStat of the specified route
	 * 
	 * @param routeId
	 * @return
	 */
	public TimeWindowStat getCurrentTimeWindowStat(String routeId) {
		long startTimeMilli = currentTimeSlotId();
		return getTimeWindowStat(routeId, startTimeMilli, startTimeMilli + 1000);
	}

	/**
	 * Returns the TimeWindowStat of previous second
	 * 
	 * @param timeMilli
	 * @return
	 */
	public TimeWindowStat getPreviousSecondStat(String routeId, long timeMilli) {
		long endTimeMilli = (timeMilli / INTERVAL) * INTERVAL;
		return getTimeWindowStat(routeId, endTimeMilli - 1000, endTimeMilli);
	}

	/**
	 * Returns the timeWindowStat of the specific route in the specified time window
	 * [startTimeMilli, endTimeMilli)
	 * 
	 * @param startTimeMilli included
	 * @param endTimemilli   excluded
	 * @return
	 */
	public TimeWindowStat getTimeWindowStat(String routeId, long startTimeMilli, long endTimeMilli) {
		long startSlotId = (startTimeMilli / INTERVAL) * INTERVAL;
		long endSlotId = (endTimeMilli / INTERVAL) * INTERVAL;

		if (startSlotId == endSlotId) {
			endSlotId = endSlotId + INTERVAL;
		}
		if (routeStats.containsKey(routeId)) {
			RouteStat routeStat = routeStats.get(routeId);
			return routeStat.getTimeWindowStat(startSlotId, endSlotId);
		}
		return null;
	}

	/**
	 * Returns the RouteTimeWindowStat list in the specified time window
	 * [startTimeMilli, endTimeMilli), The time slot unit is one second
	 * 
	 * @param routeId        optional, returns RouteSlot list of all routes while
	 *                       routeId is null
	 * @param startTimeMilli
	 * @param endTimeMilli
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<RouteTimeWindowStat> getRouteTimeWindowStats(String routeId, long startTimeMilli, long endTimeMilli) {
		return this.getRouteTimeWindowStats(routeId, startTimeMilli, endTimeMilli, INTERVAL);
	}

	/**
	 * Returns the RouteTimeWindowStat list in the specified time window
	 * [startTimeMilli, endTimeMilli)
	 * 
	 * @param routeId           optional, returns RouteTimeWindowStat list of all
	 *                          routes while routeId is null
	 * @param startTimeMilli
	 * @param endTimeMilli
	 * @param slotIntervalInSec interval of custom time slot in millisecond, such as
	 *                          60 for 1 minutes
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<RouteTimeWindowStat> getRouteTimeWindowStats(String routeId, long startTimeMilli, long endTimeMilli,
			long slotIntervalInSec) {
		List<RouteTimeWindowStat> list = new ArrayList<>();
		long startSlotId = (startTimeMilli / INTERVAL) * INTERVAL;
		long endSlotId = (endTimeMilli / INTERVAL) * INTERVAL;

		if (startSlotId == endSlotId) {
			endSlotId = endSlotId + INTERVAL;
		}
		if (slotIntervalInSec < 1 || (endSlotId - startSlotId) / 1000 < slotIntervalInSec) {
			return list;
		}
		long slotInterval = slotIntervalInSec * 1000;

		if (routeId == null) {
			Set<Map.Entry<String, RouteStat>> entrys = routeStats.entrySet();
			for (Entry<String, RouteStat> entry : entrys) {
				String rid = entry.getKey();
				RouteTimeWindowStat routeWin = new RouteTimeWindowStat(rid);
				for (long i = startSlotId; i < endSlotId;) {
					TimeWindowStat tws = getTimeWindowStat(routeId, startSlotId, endSlotId);
					if (tws != null) {
						routeWin.getWindows().add(tws);
					}
					i = i + slotInterval;
				}
				if (routeWin.getWindows().size() > 0) {
					list.add(routeWin);
				}
			}
		} else {
			RouteTimeWindowStat routeWin = new RouteTimeWindowStat(routeId);
			for (long i = startSlotId; i < endSlotId;) {
				TimeWindowStat tws = getTimeWindowStat(routeId, startSlotId, endSlotId);
				if (tws != null) {
					routeWin.getWindows().add(tws);
				}
				i = i + slotInterval;
			}
			if (routeWin.getWindows().size() > 0) {
				list.add(routeWin);
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
					Set<Map.Entry<String, RouteStat>> entrys = stat.routeStats.entrySet();
					for (Entry<String, RouteStat> entry : entrys) {
						String routeId = entry.getKey();
						ConcurrentMap<Long, TimeSlot> timeSlots = entry.getValue().getTimeSlots();
						log.debug("housekeeping remove slot: routeId={} slotId=={}", routeId, i);
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
