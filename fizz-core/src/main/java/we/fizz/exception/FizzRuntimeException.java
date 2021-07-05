package we.fizz.exception;

public class FizzRuntimeException extends RuntimeException {
	public FizzRuntimeException(String message) {
		super(message);
	}

	public FizzRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
