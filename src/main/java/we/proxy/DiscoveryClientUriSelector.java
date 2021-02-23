package we.proxy;

/**
 * A {@code DiscoveryClientUriSelector} is used to select the uri for the next request
 *
 * @author zhongjie
 */
public interface DiscoveryClientUriSelector {
    /**
     * find a instance of service by discovery and return the uri that http://{instance-ip-addr}:{instance-port}{relativeUri}
     * @param service service name
     * @param relativeUri relative uri
     * @return the uri for the next request
     */
    String getNextUri(String service, String relativeUri);

    ServiceInstance getNextInstance(String service);
}
