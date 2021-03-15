package we.proxy.grpc;

import java.util.List;

/**
 * gRPC instance service interface
 *
 * @author zhongjie
 */
public interface GrpcInstanceService {
    /**
     * random get an instance
     *
     * @param service service name
     * @return instance, {@code null} if instance not-exist
     */
    String getInstanceRandom(String service);
    /**
     * round-robin get an instance
     *
     * @param service service name
     * @return instance, {@code null} if instance not-exist
     */
    String getInstanceRoundRobin(String service);
    /**
     *  get all instances
     *
     * @param service service name
     * @return instance, {@code null} if instance not-exist
     */
    List<String> getAllInstance(String service);
}
