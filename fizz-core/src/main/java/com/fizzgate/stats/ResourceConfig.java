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

package com.fizzgate.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Francis Dong
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceConfig {

	public ResourceConfig(String resourceId, long maxCon, long maxQPS) {
		this.resourceId = resourceId;
		this.maxCon = maxCon;
		this.maxQPS = maxQPS;
	}

	/**
	 * Resouce ID
	 */
	private String resourceId;

	//---------------------------------------------------------------------
	// Flow control rule
	//---------------------------------------------------------------------
	/**
	 * Maximum concurrent request, zero or negative for no limit
	 */
	private long maxCon;

	/**
	 * Maximum QPS, zero or negative for no limit
	 */
	private long maxQPS;


	//---------------------------------------------------------------------
	// Degrade rule
	//---------------------------------------------------------------------
	/**
	 * Degrade strategy: 1-exception ratio 2-exception count
	 */
	private Byte strategy;
	/**
	 * Ratio threshold, not null when degrade strategy is 1-exception ratio
	 */
	private Float ratioThreshold;
	/**
	 * Exception count, not null when degrade strategy is 2-exception count
	 */
	private Long exceptionCount;
	/**
	 * Minimal request count
	 */
	private Long minRequestCount;
	/**
	 * Time window(second)
	 */
	private Integer timeWindow;
	/**
	 * Statistic interval(second)
	 */
	private Integer statInterval;
	/**
	 * Recovery strategy: 1-try one 2-recover gradually 3-recover immediately
	 */
	private Byte recoveryStrategy;
	/**
	 * Recovery time window(second)，not null when recovery strategy is 2-recover gradually
	 */
	private Integer recoveryTimeWindow;
}
