package we.stats;

import java.math.BigDecimal;

/**
 * 
 * @author Francis Dong
 *
 */
public class TimeWindowStat {

	/**
	 * Start time of time window，[startTime,endTime)
	 */
	private Long startTime;

	/**
	 * End time of time window, [startTime,endTime)
	 */
	private Long endTime;

	/**
	 * Minimum response time
	 */
	private Long min;

	/**
	 * Maximum response time
	 */
	private Long max;

	/**
	 * Average response time
	 */
	private Long avgRt;

	/**
	 * Total requests
	 */
	private Long total;

	/**
	 * Total error requests
	 */
	private Long errors;

	/**
	 * the average RPS(Requests Per Second) of time window
	 */
	private BigDecimal rps;
	
	/**
	 * Peak concurrent requests of the time window
	 */
	private Integer peakConcurrentReqeusts;
	
	public Integer getPeakConcurrentReqeusts() {
		return peakConcurrentReqeusts;
	}

	public void setPeakConcurrentReqeusts(Integer peakConcurrentReqeusts) {
		this.peakConcurrentReqeusts = peakConcurrentReqeusts;
	}

	public Long getErrors() {
		return errors;
	}

	public void setErrors(Long errors) {
		this.errors = errors;
	}

	public Long getMin() {
		return min;
	}

	public void setMin(Long min) {
		this.min = min;
	}

	public Long getMax() {
		return max;
	}

	public void setMax(Long max) {
		this.max = max;
	}

	public BigDecimal getRps() {
		return rps;
	}

	public void setRps(BigDecimal rps) {
		this.rps = rps;
	}

	public Long getAvgRt() {
		return avgRt;
	}

	public void setAvgRt(Long avgRt) {
		this.avgRt = avgRt;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

}
