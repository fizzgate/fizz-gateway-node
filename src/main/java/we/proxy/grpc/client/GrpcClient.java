/*
MIT License

Copyright (c) 2018 liuzhengyang

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package we.proxy.grpc.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
//import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static we.proxy.grpc.client.utils.GrpcReflectionUtils.fetchFullMethodName;
import static we.proxy.grpc.client.utils.GrpcReflectionUtils.fetchMethodType;

import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;

import io.grpc.ClientCall;
import io.grpc.MethodDescriptor.MethodType;

import io.grpc.stub.StreamObserver;
import we.proxy.grpc.client.core.CompositeStreamObserver;
import we.proxy.grpc.client.core.DoneObserver;
import we.proxy.grpc.client.core.DynamicMessageMarshaller;

/**
 * @author zhangjikai
 */
public class GrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);

    @Nullable
    public ListenableFuture<Void> call(CallParams callParams) {
        checkParams(callParams);
        MethodType methodType = fetchMethodType(callParams.getMethodDescriptor());
        List<DynamicMessage> requests = callParams.getRequests();
        StreamObserver<DynamicMessage> responseObserver = callParams.getResponseObserver();
        DoneObserver<DynamicMessage> doneObserver = new DoneObserver<>();
        StreamObserver<DynamicMessage> compositeObserver = CompositeStreamObserver.of(responseObserver, doneObserver);
        StreamObserver<DynamicMessage> requestObserver;
        switch (methodType) {
            case UNARY:
                asyncUnaryCall(createCall(callParams), requests.get(0), compositeObserver);
                return doneObserver.getCompletionFuture();
            case SERVER_STREAMING:
                asyncServerStreamingCall(createCall(callParams), requests.get(0), compositeObserver);
                return doneObserver.getCompletionFuture();
            case CLIENT_STREAMING:
                requestObserver = asyncClientStreamingCall(createCall(callParams), compositeObserver);
                requests.forEach(responseObserver::onNext);
                requestObserver.onCompleted();
                return doneObserver.getCompletionFuture();
            case BIDI_STREAMING:
                requestObserver = asyncBidiStreamingCall(createCall(callParams), compositeObserver);
                requests.forEach(responseObserver::onNext);
                requestObserver.onCompleted();
                return doneObserver.getCompletionFuture();
            default:
                logger.info("Unknown methodType:{}", methodType);
                return null;
        }
    }

    private void checkParams(CallParams callParams) {
        checkNotNull(callParams);
        checkNotNull(callParams.getMethodDescriptor());
        checkNotNull(callParams.getChannel());
        checkNotNull(callParams.getCallOptions());
        checkArgument(isNotEmpty(callParams.getRequests()));
        checkNotNull(callParams.getResponseObserver());
    }

    private ClientCall<DynamicMessage, DynamicMessage> createCall(CallParams callParams) {
        return callParams.getChannel().newCall(createGrpcMethodDescriptor(callParams.getMethodDescriptor()),
                callParams.getCallOptions());
    }

    private io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> createGrpcMethodDescriptor(MethodDescriptor descriptor) {
        return io.grpc.MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(fetchMethodType(descriptor))
                .setFullMethodName(fetchFullMethodName(descriptor))
                .setRequestMarshaller(new DynamicMessageMarshaller(descriptor.getInputType()))
                .setResponseMarshaller(new DynamicMessageMarshaller(descriptor.getOutputType()))
                .build();
    }
}
