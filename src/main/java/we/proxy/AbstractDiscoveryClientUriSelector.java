package we.proxy;

import we.util.Constants;
import we.util.ThreadContext;

/**
 * Abstract implementation of {@code DiscoveryClientUriSelector}
 *
 * @author zhongjie
 */
abstract public class AbstractDiscoveryClientUriSelector implements DiscoveryClientUriSelector {

    protected String buildUri(String ipAddr, int port, String path) {
        StringBuilder b = ThreadContext.getStringBuilder();
        return b.append(Constants.Symbol.HTTP_PROTOCOL_PREFIX).append(ipAddr).append(Constants.Symbol.COLON).append(port).append(path).toString();
    }
}
