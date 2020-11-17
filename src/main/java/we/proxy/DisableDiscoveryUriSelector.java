package we.proxy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * The disable implementation of {@code DiscoveryClientUriSelector}, used when Nacos and Eureka discovery are not enabled.
 *
 * @author zhongjie
 */

@ConditionalOnExpression("${nacos.discovery.enabled} == false and ${eureka.client.enabled} == false")
@Service
public class DisableDiscoveryUriSelector implements DiscoveryClientUriSelector {
    @Override
    public String getNextUri(String service, String relativeUri) {
        throw new RuntimeException("No " + service + " because discovery disabled", null, false, false) {};
    }
}
