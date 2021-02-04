package we.fizz.input.extension.grpc;

import org.springframework.http.HttpStatus;
import we.fizz.input.RPCResponse;

public class GRPCResponse extends RPCResponse {
    private HttpStatus statusCode;
    public void setStatus(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
