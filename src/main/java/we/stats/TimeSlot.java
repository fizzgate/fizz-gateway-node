package we.stats;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Francis Dong
 *
 */
public class TimeSlot {

	/**
	 * Time slot start timestamp as ID
	 */
	private long id;

	/**
	 * Request counter
	 */
	private AtomicLong counter = new AtomicLong();

	/**
	 * Error request counter
	 */
	private AtomicLong errors = new AtomicLong();

	/**
	 * Minimum response time
	 */
	private long min = Long.MAX_VALUE;

	/**
	 * Maximum response time
	 */
	private long max = Long.MIN_VALUE;

	/**
	 * Total response time
	 */
	private AtomicLong totalRts = new AtomicLong(0);

	/**
	 * Peak concurrent requests
	 */
	private int peakConcurrentReqeusts;

	public TimeSlot(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	/**
	 * Add request to time slot
	 * 
	 * @param rt
	 * @param concurrentRequests Current concurrent requests
	 * @param isSuccess          Whether the request is success or not
	 */
	public synchronized void incr(long rt, int concurrentRequests, boolean isSuccess) {
		counter.incrementAndGet();
		totalRts.addAndGet(rt);
		if (!isSuccess) {
			errors.incrementAndGet();
		}
		min = rt < min ? rt : min;
		max = rt > max ? rt : max;
		peakConcurrentReqeusts = concurrentRequests > peakConcurrentReqeusts ? concurrentRequests
				: peakConcurrentReqeusts;
	}

	public void setId(long id) {
		this.id = id;
	}

	public AtomicLong getCounter() {
		return counter;
	}

	public void setCounter(AtomicLong counter) {
		this.counter = counter;
	}

	public long getMin() {
		return min;
	}

	public void setMin(long min) {
		this.min = min;
	}

	public long getMax() {
		return max;
	}

	public void setMax(long max) {
		this.max = max;
	}

	public AtomicLong getTotalRts() {
		return totalRts;
	}

	public void setTotalRts(AtomicLong totalRts) {
		this.totalRts = totalRts;
	}

	public int getPeakConcurrentReqeusts() {
		return peakConcurrentReqeusts;
	}

	public void setPeakConcurrentReqeusts(int peakConcurrentReqeusts) {
		this.peakConcurrentReqeusts = peakConcurrentReqeusts;
	}

	public AtomicLong getErrors() {
		return errors;
	}

	public void setErrors(AtomicLong errors) {
		this.errors = errors;
	}

}
