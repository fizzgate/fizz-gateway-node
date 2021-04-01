package we.proxy.grpc.client.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
//import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.TypeRegistry;

import io.grpc.Channel;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.Status;
import we.proxy.grpc.client.core.GrpcMethodDefinition;
import we.proxy.grpc.client.core.ServerReflectionClient;

/**
 * @author zhangjikai
 */
public class GrpcReflectionUtils {
    private static final Logger logger = LoggerFactory.getLogger(GrpcReflectionUtils.class);

    public static List<FileDescriptorSet> resolveServices(Channel channel) {
        ServerReflectionClient serverReflectionClient = ServerReflectionClient.create(channel);
        try {
            List<String> services = serverReflectionClient.listServices().get();
            if (isEmpty(services)) {
                logger.info("Can't find services by channel {}", channel);
                return emptyList();
            }
            return services.stream().map(serviceName -> {
                ListenableFuture<FileDescriptorSet> future = serverReflectionClient.lookupService(serviceName);
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Get {} fileDescriptor occurs error", serviceName, e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(toList());
        } catch (Throwable t) {
            logger.error("Exception resolve service", t);
            throw new RuntimeException(t);
        }
    }

    public static FileDescriptorSet resolveService(Channel channel, String serviceName) {
        ServerReflectionClient reflectionClient = ServerReflectionClient.create(channel);
        try {
            List<String> serviceNames = reflectionClient.listServices().get();
            if (!serviceNames.contains(serviceName)) {
                throw Status.NOT_FOUND.withDescription(
                        String.format("Remote server does not have service %s. Services: %s", serviceName, serviceNames))
                        .asRuntimeException();
            }

            return reflectionClient.lookupService(serviceName).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Resolve services get error", e);
            throw new RuntimeException(e);
        }
    }

    public static String fetchFullMethodName(MethodDescriptor methodDescriptor) {
        String serviceName = methodDescriptor.getService().getFullName();
        String methodName = methodDescriptor.getName();
        return generateFullMethodName(serviceName, methodName);
    }

    public static MethodType fetchMethodType(MethodDescriptor methodDescriptor) {
        boolean clientStreaming = methodDescriptor.toProto().getClientStreaming();
        boolean serverStreaming = methodDescriptor.toProto().getServerStreaming();
        if (clientStreaming && serverStreaming) {
            return MethodType.BIDI_STREAMING;
        } else if (!clientStreaming && !serverStreaming) {
            return MethodType.UNARY;
        } else if (!clientStreaming) {
            return MethodType.SERVER_STREAMING;
        } else {
            return MethodType.SERVER_STREAMING;
        }
    }

    public static List<DynamicMessage> parseToMessages(TypeRegistry registry, Descriptor descriptor,
            List<String> jsonTexts) {
        Parser parser = JsonFormat.parser().usingTypeRegistry(registry);
        List<DynamicMessage> messages = new ArrayList<>();
        try {
            for (String jsonText : jsonTexts) {
                DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(descriptor);
                parser.merge(jsonText, messageBuilder);
                messages.add(messageBuilder.build());
            }
            return messages;
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Unable to parse json text", e);
        }
    }

    public static GrpcMethodDefinition parseToMethodDefinition(String rawMethodName) {
        checkArgument(isNotBlank(rawMethodName), "Raw method name can't be empty.");
        int methodSplitPosition = rawMethodName.lastIndexOf(".");
        checkArgument(methodSplitPosition != -1, "No package name and service name found.");
        String methodName = rawMethodName.substring(methodSplitPosition + 1);
        checkArgument(isNotBlank(methodName), "Method name can't be empty.");
        String fullServiceName = rawMethodName.substring(0, methodSplitPosition);
        int serviceSplitPosition = fullServiceName.lastIndexOf(".");
        String serviceName = fullServiceName.substring(serviceSplitPosition + 1);
        String packageName = "";
        if (serviceSplitPosition != -1) {
            packageName = fullServiceName.substring(0, serviceSplitPosition);
        }
        checkArgument(isNotBlank(serviceName), "Service name can't be empty.");
        return new GrpcMethodDefinition(packageName, serviceName, methodName);
    }
}
