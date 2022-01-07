package we.stats.circuitbreaker;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.Map;

@Component
public class CircuitBreakManager {

    private Map<String, CircuitBreaker> circuitBreakerMap = new HashMap<>(32);

    public CircuitBreaker create(Object circuitBreakerConfig) {
        // circuitBreakerConfig => CircuitBreaker
        circuitBreakerMap.put("", null/*CircuitBreaker*/);
        return null;
    }

    public boolean cutOff(ServerWebExchange exchange, String service, String path) {
        return false;
    }
}
