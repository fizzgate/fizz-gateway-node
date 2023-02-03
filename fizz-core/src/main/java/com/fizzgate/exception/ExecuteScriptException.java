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

package com.fizzgate.exception;

import com.fizzgate.fizz.StepContext;

/**
 * @author Francis
 */
public class ExecuteScriptException extends RuntimeException {

	private StepContext<String, Object> stepContext;

	private Object data;

	public ExecuteScriptException(String message, StepContext<String, Object> stepContext, Object data) {
		super(message);
		this.data = data;
		this.stepContext = stepContext;
		this.stepContext.setExceptionInfo(this, data);
	}

	public ExecuteScriptException(Throwable cause, StepContext<String, Object> stepContext, Object data) {
		super("execute script failed: " + cause.getMessage(), cause);
		this.data = data;
		this.stepContext = stepContext;
		this.setStackTrace(cause.getStackTrace());
		this.stepContext.setExceptionInfo(this, data);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public StepContext<String, Object> getStepContext() {
		return stepContext;
	}

	public void setStepContext(StepContext<String, Object> stepContext) {
		this.stepContext = stepContext;
	}

}
