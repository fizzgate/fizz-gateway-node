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

import we.stats.circuitbreaker.CircuitBreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
//	private AtomicLong counter = new AtomicLong();
	private volatile int counter = 0;

	/**
	 * Error request counter
	 */
//	private AtomicLong errors = new AtomicLong();
	private volatile int errors = 0;

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
//	private AtomicLong totalRt = new AtomicLong(0);
	private volatile long totalRt = 0;
	
	/**
	 * Completed Request counter
	 */
//	private AtomicLong compReqs = new AtomicLong();
	private volatile int compReqs = 0;
	

	/**
	 * Peak concurrent requests
	 */
	private long peakConcurrentRequests;

	/**
	 * Block requests <br/>
	 */
//	private AtomicLong blockRequests = new AtomicLong(0);
	private volatile int blockRequests = 0;
	
	/**
	 * Total block requests of the resource and its underlying resources <br/>
	 */
//	private AtomicLong totalBlockRequests = new AtomicLong(0);
	private volatile long totalBlockRequests = 0;

	private AtomicReference<CircuitBreaker.State> circuitBreakState   = new AtomicReference<>(CircuitBreaker.State.CLOSED);

//	private AtomicLong                            circuitBreakNum     = new AtomicLong(0);
	private volatile int                          circuitBreakNum     = 0;

//	private AtomicLong                            gradualResumeNum    = new AtomicLong(0);
	private volatile int                          gradualResumeNum    = 0;

//	private AtomicInteger                         resumeTrafficFactor = new AtomicInteger(1);
	private volatile int                          resumeTrafficFactor = 1;

//	private AtomicLong                            gradualRejectNum    = new AtomicLong(0);
	private volatile int                          gradualRejectNum    = 0;

	private AtomicInteger                         _2xxStatusCount     = new AtomicInteger(0);

	private AtomicInteger                         _4xxStatusCount     = new AtomicInteger(0);

	private AtomicInteger                         _5xxStatusCount     = new AtomicInteger(0);

	private AtomicInteger                         _504StatusCount     = new AtomicInteger(0);

	public AtomicInteger get2xxStatusCount() {
		return _2xxStatusCount;
	}

	public AtomicInteger get4xxStatusCount() {
		return _4xxStatusCount;
	}

	public AtomicInteger get5xxStatusCount() {
		return _5xxStatusCount;
	}

	public AtomicInteger get504StatusCount() {
		return _504StatusCount;
	}

	public AtomicReference<CircuitBreaker.State> getCircuitBreakState() {
		return circuitBreakState;
	}

	public int getCircuitBreakNum() {
		return circuitBreakNum;
	}

	public void setCircuitBreakNum(int v) {
		circuitBreakNum = v;
	}

	public void incrCircuitBreakNum() {
		++circuitBreakNum;
	}

	public int getGradualResumeNum() {
		return gradualResumeNum;
	}

	public int incrGradualResumeNum() {
		return ++gradualResumeNum;
	}

	public int decrGradualResumeNum() {
		return --gradualResumeNum;
	}

	public int getResumeTrafficFactor() {
		return resumeTrafficFactor;
	}

	public void incrResumeTrafficFactor() {
		++resumeTrafficFactor;
	}

	public int getGradualRejectNum() {
		return gradualRejectNum;
	}

	public int incrGradualRejectNum() {
		return ++gradualRejectNum;
	}

	public int decrGradualRejectNum() {
		return --gradualRejectNum;
	}


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
		// counter.incrementAndGet();
		++counter;
	}

	/**
	 * Add request RT information to time slot
	 * 
	 * @param rt
	 * @param isSuccess Whether the request is success or not
	 */
	public synchronized void addRequestRT(long rt, boolean isSuccess) {
		totalRt += rt;
		++compReqs;
		if (!isSuccess) {
			++errors;
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
		peakConcurrentRequests = concurrentRequests > peakConcurrentRequests ? concurrentRequests
				: peakConcurrentRequests;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
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

	public long getTotalRt() {
		return totalRt;
	}

	public void setTotalRt(long totalRt) {
		this.totalRt = totalRt;
	}

	public long getPeakConcurrentRequests() {
		return peakConcurrentRequests;
	}

	public void setPeakConcurrentRequests(long peakConcurrentRequests) {
		this.peakConcurrentRequests = peakConcurrentRequests;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

	public int getBlockRequests() {
		return blockRequests;
	}

	public void setBlockRequests(int blockRequests) {
		this.blockRequests = blockRequests;
	}

	public void incrBlockRequests() {
		++blockRequests;
	}

	public int getCompReqs() {
		return compReqs;
	}

	public void setCompReqs(int compReqs) {
		this.compReqs = compReqs;
	}

	public long getTotalBlockRequests() {
		return totalBlockRequests;
	}

	public void incrTotalBlockRequests() {
		++totalBlockRequests;
	}

	public void setTotalBlockRequests(long totalBlockRequests) {
		this.totalBlockRequests = totalBlockRequests;
	}

}
