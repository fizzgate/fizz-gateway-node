package we.fizz.exception;

public class FizzException extends Exception {
    public FizzException(Throwable exception) {
        super(exception);
    }
    public FizzException(String message) {
        super(message);
    }
}
