package we.proxy.grpc.client.utils;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.util.Collections.emptyMap;

import java.util.Map;

import com.google.common.net.HostAndPort;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors.CheckedForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;

/**
 * Knows how to construct grpc channels.
 */
public class ChannelFactory {

    public static ManagedChannel create(HostAndPort endpoint) {
        return create(endpoint, emptyMap());
    }

    public static ManagedChannel create(HostAndPort endpoint, Map<String, Object> metaDataMap) {
        return NettyChannelBuilder.forAddress(endpoint.getHostText(), endpoint.getPort())
                .negotiationType(NegotiationType.PLAINTEXT)
                .intercept(metadataInterceptor(metaDataMap))
                .build();
    }

    private static ClientInterceptor metadataInterceptor(Map<String, Object> metaDataMap) {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    final MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, final Channel next) {

                return new CheckedForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                    @Override
                    protected void checkedStart(Listener<RespT> responseListener, Metadata headers) {
                        metaDataMap.forEach((k, v) -> {
                            Key<String> mKey = Key.of(k, ASCII_STRING_MARSHALLER);
                            headers.put(mKey, String.valueOf(v));
                        });
                        delegate().start(responseListener, headers);
                    }
                };
            }
        };
    }
}
