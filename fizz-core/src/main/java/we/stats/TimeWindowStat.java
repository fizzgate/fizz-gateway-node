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
	 * Completed requests
	 */
	private Long compReqs;

	/**
	 * Total error requests
	 */
	private Long errors;

	/**
	 * the average RPS(Requests Per Second) of time window
	 */
	private BigDecimal rps;
	
	/**
	 * the peak RPS(Requests Per Second) of time window
	 */
	private BigDecimal peakRps;

	/**
	 * Peak concurrent requests of the time window
	 */
	private Long peakConcurrentReqeusts;

	/**
	 * Block requests
	 */
	private Long blockRequests;
	
	/**
	 * Total block requests
	 */
	private Long totalBlockRequests;

	private int _2xxStatus = 0;
	private int _4xxStatus = 0;
	private int _5xxStatus = 0;
	private int _504Status = 0;

	public Long getBlockRequests() {
		return blockRequests;
	}

	public void setBlockRequests(Long blockRequests) {
		this.blockRequests = blockRequests;
	}

	public Long getPeakConcurrentReqeusts() {
		return peakConcurrentReqeusts;
	}

	public void setPeakConcurrentReqeusts(Long peakConcurrentReqeusts) {
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

	public Long getCompReqs() {
		return compReqs;
	}

	public void setCompReqs(Long compReqs) {
		this.compReqs = compReqs;
	}

	public Long getTotalBlockRequests() {
		return totalBlockRequests;
	}

	public void setTotalBlockRequests(Long totalBlockRequests) {
		this.totalBlockRequests = totalBlockRequests;
	}

	public BigDecimal getPeakRps() {
		return peakRps;
	}

	public void setPeakRps(BigDecimal peakRps) {
		this.peakRps = peakRps;
	}

	public void set2xxStatus(int _2xxStatus) {
		this._2xxStatus = _2xxStatus;
	}

	public void set4xxStatus(int _4xxStatus) {
		this._4xxStatus = _4xxStatus;
	}

	public void set5xxStatus(int _5xxStatus) {
		this._5xxStatus = _5xxStatus;
	}

	public void set504Status(int _504status) {
		this._504Status = _504status;
	}

	public int get2xxStatus() {
		return _2xxStatus;
	}

	public int get4xxStatus() {
		return _4xxStatus;
	}

	public int get5xxStatus() {
		return _5xxStatus;
	}

	public int get504Status() {
		return _504Status;
	}
}
