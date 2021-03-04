package we.fizz.input.extension.request;

import org.springframework.http.HttpStatus;
import we.fizz.input.RPCResponse;

public class RequestRPCResponse extends RPCResponse {
    private HttpStatus statusCode;
    public void setStatus(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
