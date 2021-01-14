package we.proxy.grpc;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import org.apache.dubbo.rpc.service.GenericException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import we.fizz.exception.FizzException;
import static io.grpc.CallOptions.DEFAULT;
import static java.util.Collections.singletonList;

import we.proxy.grpc.client.GrpcProxyClient;
import we.proxy.grpc.client.core.GrpcMethodDefinition;
import we.proxy.grpc.client.utils.ChannelFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static we.proxy.grpc.client.utils.GrpcReflectionUtils.parseToMethodDefinition;

@Service
public class GrpcGenericService {

    @Autowired
    private GrpcProxyClient grpcProxyClient;
    /**
     * Generic invoke.
     *
     * @param payload     the json string body
     * @param grpcInterfaceDeclaration the interface declaration
     * @return the mono object
     * @throws FizzException the fizz runtime exception
     */
    public Mono<Object> send(final String payload, final GrpcInterfaceDeclaration grpcInterfaceDeclaration, HashMap<String, Object> attachments ) {
        GrpcMethodDefinition methodDefinition = parseToMethodDefinition(grpcInterfaceDeclaration.getServiceName());
        HostAndPort endPoint = HostAndPort.fromString(grpcInterfaceDeclaration.getEndpoint());
        if (endPoint == null) {
            throw new RuntimeException("can't find target endpoint");
        }
        Map<String, Object> metaHeaderMap = attachments;
        ManagedChannel channel = null;
        try {
            channel = ChannelFactory.create(endPoint, metaHeaderMap);
            CallOptions calloptions = DEFAULT;
            calloptions.withDeadlineAfter(grpcInterfaceDeclaration.getTimeout(), TimeUnit.MILLISECONDS);
            ListenableFuture<Void> future = grpcProxyClient.invokeMethodAsync(methodDefinition, channel, DEFAULT, singletonList(payload));
            return Mono.fromFuture(new ListenableFutureAdapter(future).getCompletableFuture().thenApply(ret -> {
                return ret;
            })).onErrorMap(
                    exception -> exception instanceof GenericException ? new FizzException(((GenericException) exception).getExceptionMessage()) : new FizzException((Throwable) exception));
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }
}
