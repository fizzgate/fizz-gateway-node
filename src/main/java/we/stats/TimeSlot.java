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
	private AtomicLong totalRt = new AtomicLong(0);
	
	/**
	 * Completed Request counter
	 */
	private AtomicLong compReqs = new AtomicLong();
	

	/**
	 * Peak concurrent requests
	 */
	private long peakConcurrentReqeusts;

	/**
	 * Block requests <br/>
	 */
	private AtomicLong blockRequests = new AtomicLong(0);

	public TimeSlot(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	/**
	 * Add request to time slot
	 * 
	 */
	public void incr() {
		counter.incrementAndGet();
	}

	/**
	 * Add request RT information to time slot
	 * 
	 * @param rt
	 * @param isSuccess Whether the request is success or not
	 */
	public synchronized void addRequestRT(long rt, boolean isSuccess) {
		totalRt.addAndGet(rt);
		compReqs.incrementAndGet();
		if (!isSuccess) {
			errors.incrementAndGet();
		}
		min = rt < min ? rt : min;
		max = rt > max ? rt : max;
	}

	/**
	 * Update peak concurrent requests of this time slot
	 * 
	 * @param concurrentRequests Current concurrent requests
	 */
	public synchronized void updatePeakConcurrentReqeusts(long concurrentRequests) {
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

	public AtomicLong getTotalRt() {
		return totalRt;
	}

	public void setTotalRt(AtomicLong totalRt) {
		this.totalRt = totalRt;
	}

	public long getPeakConcurrentReqeusts() {
		return peakConcurrentReqeusts;
	}

	public void setPeakConcurrentReqeusts(long peakConcurrentReqeusts) {
		this.peakConcurrentReqeusts = peakConcurrentReqeusts;
	}

	public AtomicLong getErrors() {
		return errors;
	}

	public void setErrors(AtomicLong errors) {
		this.errors = errors;
	}

	public AtomicLong getBlockRequests() {
		return blockRequests;
	}

	public void setBlockRequests(AtomicLong blockRequests) {
		this.blockRequests = blockRequests;
	}

	public AtomicLong getCompReqs() {
		return compReqs;
	}

	public void setCompReqs(AtomicLong compReqs) {
		this.compReqs = compReqs;
	}

}
