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
package com.fizzgate.proxy.grpc.client;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fizzgate.proxy.grpc.client.core.GrpcMethodDefinition;
import com.fizzgate.proxy.grpc.client.core.ServiceResolver;
import com.fizzgate.proxy.grpc.client.utils.GrpcReflectionUtils;
import com.fizzgate.proxy.grpc.client.utils.MessageWriter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat.TypeRegistry;

import io.grpc.CallOptions;
import io.grpc.Channel;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
@Service
/**
 * @author zhangjikai
 * Created on 2018-12-01
 */
public class GrpcProxyClient {
	
    private GrpcClient grpcClient = new GrpcClient();
    
    public CallResults invokeMethod(GrpcMethodDefinition definition, Channel channel, CallOptions callOptions,
                                         List<String> requestJsonTexts) {
        CallResults results = new CallResults();
        try {
            this.invokeMethodAsync( definition,  channel,  callOptions, requestJsonTexts, results).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Caught exception while waiting for rpc", e);
        }
        return results;
    }

    public ListenableFuture<Void> invokeMethodAsync(GrpcMethodDefinition definition, Channel channel, CallOptions callOptions,
                                                    List<String> requestJsonTexts, CallResults results) {
        FileDescriptorSet fileDescriptorSet = GrpcReflectionUtils.resolveService(channel, definition.getFullServiceName());
        if (fileDescriptorSet == null) {
            return null;
        }
        ServiceResolver serviceResolver = ServiceResolver.fromFileDescriptorSet(fileDescriptorSet);
        MethodDescriptor methodDescriptor = serviceResolver.resolveServiceMethod(definition);
        TypeRegistry registry = TypeRegistry.newBuilder().add(serviceResolver.listMessageTypes()).build();
        List<DynamicMessage> requestMessages = GrpcReflectionUtils.parseToMessages(registry, methodDescriptor.getInputType(),
                requestJsonTexts);
//        CallResults results = new CallResults();
        StreamObserver<DynamicMessage> streamObserver = MessageWriter.newInstance(registry, results);
        CallParams callParams = CallParams.builder()
                .methodDescriptor(methodDescriptor)
                .channel(channel)
                .callOptions(callOptions)
                .requests(requestMessages)
                .responseObserver(streamObserver)
                .build();

        return grpcClient.call(callParams);

    }
}
