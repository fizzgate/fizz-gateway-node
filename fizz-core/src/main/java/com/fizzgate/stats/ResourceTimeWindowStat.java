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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Francis Dong
 *
 */
public class ResourceTimeWindowStat {

	/**
	 * Resource ID
	 */
	private String resourceId;

	private List<TimeWindowStat> windows = new ArrayList<>();

	public ResourceTimeWindowStat(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public List<TimeWindowStat> getWindows() {
		return windows;
	}

	public void setWindows(List<TimeWindowStat> windows) {
		this.windows = windows;
	}

}
