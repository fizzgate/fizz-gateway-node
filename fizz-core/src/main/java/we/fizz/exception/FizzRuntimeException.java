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

package we.fizz.exception;

import we.fizz.StepContext;

public class FizzRuntimeException extends RuntimeException {

	private StepContext<String, Object> stepContext;

	public FizzRuntimeException(String message) {
		super(message);
	}

	public FizzRuntimeException(String message, Throwable cause) {
		super(message, cause);
		this.setStackTrace(cause.getStackTrace());
	}

	public FizzRuntimeException(String message, StepContext<String, Object> stepContext) {
		super(message);
		this.stepContext = stepContext;
	}

	public FizzRuntimeException(String message, Throwable cause, StepContext<String, Object> stepContext) {
		super(message, cause);
		this.setStackTrace(cause.getStackTrace());
		this.stepContext = stepContext;
	}

	public StepContext<String, Object> getStepContext() {
		return stepContext;
	}

	public void setStepContext(StepContext<String, Object> stepContext) {
		this.stepContext = stepContext;
	}

}
