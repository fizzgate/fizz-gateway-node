/*  Copyright (C) 2021 the original author or authors.
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

package we.fizz.component;

import lombok.Data;

/**
 * 
 * @author Francis Dong
 *
 */
@Data
public class StepContextPosition {

	private String stepName;
	private String requestName;

	public StepContextPosition(String stepName) {
		this.stepName = stepName;
	}

	public StepContextPosition(String stepName, String requestName) {
		this.stepName = stepName;
		this.requestName = requestName;
	}

	public String getPath() {
		if (this.requestName == null) {
			return this.stepName;
		}
		return this.stepName + ".requests." + this.requestName;
	}

}
