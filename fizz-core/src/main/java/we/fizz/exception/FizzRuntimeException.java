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
