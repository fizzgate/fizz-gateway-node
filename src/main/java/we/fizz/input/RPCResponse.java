package we.fizz.input;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 *
 * @author linwaiwai
 *
 */
public class RPCResponse {
    private MultiValueMap headers;
    private Mono<String> bodyMono;

    public MultiValueMap getHeaders() {
        return headers;
    }

    public void setHeaders(MultiValueMap headers) {
        this.headers = headers;
    }

    public Mono<String> getBodyMono() {
        return bodyMono;
    }

    public void setBodyMono(Mono<String> bodyMono) {
        this.bodyMono = bodyMono;
    }
}
