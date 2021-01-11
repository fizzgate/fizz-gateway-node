package we.fizz.input;

import org.springframework.http.HttpStatus;

public class RequestRPCResponse extends RPCResponse {
    private HttpStatus statusCode;
    public void setStatus(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
