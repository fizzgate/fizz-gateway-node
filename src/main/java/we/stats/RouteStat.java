package we.stats;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author Francis Dong
 *
 */
public class RouteStat {

	/**
	 * Route ID
	 */
	private String routeId;

	/**
	 * Request count of time slot, the beginning timestamp(timeId) as key
	 */
	private ConcurrentMap<Long, TimeSlot> timeSlots = new ConcurrentHashMap<>();

	/**
	 * Concurrent requests
	 */
	private AtomicInteger concurrentRequests = new AtomicInteger(0);

	public RouteStat(String routeId) {
		this.routeId = routeId;
	}

	/**
	 * Increment concurrent request counter of the route
	 * 
	 */
	public void incrConcurrentRequest() {
		this.concurrentRequests.incrementAndGet();
	}

	/**
	 * add request to the specified time slot and decrement concurrent request
	 * counter
	 * 
	 * @param timeSlotId
	 * @param rt         response time of the request
	 * @param isSuccess  Whether the request is success or not
	 * @return
	 */
	public void incrRequestToTimeSlot(long timeSlotId, long rt, boolean isSuccess) {
		int conns = this.concurrentRequests.decrementAndGet();
		if (timeSlots.containsKey(timeSlotId)) {
			timeSlots.get(timeSlotId).incr(rt, conns, isSuccess);
		} else {
			TimeSlot timeSlot = new TimeSlot(timeSlotId);
			TimeSlot old = timeSlots.putIfAbsent(timeSlotId, timeSlot);
			if (old != null) {
				old.incr(rt, conns, isSuccess);
			} else {
				timeSlot.incr(rt, conns, isSuccess);
			}
		}
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

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		long totalReqs = 0;
		long totalRts = 0;
		int peakConcurrences = 0;
		long errors = 0;
		for (long i = startSlotId; i < endSlotId;) {
			if (timeSlots.containsKey(i)) {
				TimeSlot timeSlot = timeSlots.get(i);
				min = timeSlot.getMin() < min ? timeSlot.getMin() : min;
				max = timeSlot.getMax() > max ? timeSlot.getMax() : max;
				peakConcurrences = timeSlot.getPeakConcurrentReqeusts() > peakConcurrences
						? timeSlot.getPeakConcurrentReqeusts()
						: peakConcurrences;
				totalReqs = totalReqs + timeSlot.getCounter().get();
				totalRts = totalRts + timeSlot.getTotalRts().get();
				errors = errors + timeSlot.getErrors().get();
			}
			i = i + FlowStat.INTERVAL;
		}
		tws.setMin(min == Long.MAX_VALUE ? null : min);
		tws.setMax(max == Long.MIN_VALUE ? null : max);
		tws.setPeakConcurrentReqeusts(peakConcurrences);
		tws.setTotal(totalReqs);
		tws.setErrors(errors);

		if (totalReqs > 0) {
			tws.setAvgRt(totalRts / totalReqs);

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

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public ConcurrentMap<Long, TimeSlot> getTimeSlots() {
		return timeSlots;
	}

	public void setTimeSlots(ConcurrentMap<Long, TimeSlot> timeSlots) {
		this.timeSlots = timeSlots;
	}

	public AtomicInteger getConcurrentRequests() {
		return concurrentRequests;
	}

	public void setConcurrentRequests(AtomicInteger concurrentRequests) {
		this.concurrentRequests = concurrentRequests;
	}

}
