/*
Copyright (c) 2016, gRPC Ecosystem
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of polyglot nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fizzgate.proxy.grpc.client.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.Channel;
import io.grpc.reflection.v1alpha.ListServiceResponse;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServerReflectionResponse.MessageResponseCase;
import io.grpc.stub.StreamObserver;

public class ServerReflectionClient {
    private static final Logger logger = LoggerFactory.getLogger(ServerReflectionClient.class);
    private static final long LIST_RPC_DEADLINE_MS = 10_000;
    private static final long LOOKUP_RPC_DEADLINE_MS = 10_000;
    private static final ServerReflectionRequest LIST_SERVICES_REQUEST =
            ServerReflectionRequest.newBuilder()
                    .setListServices("")  // Not sure what this is for, appears to be ignored.
                    .build();

    private final Channel channel;

    /**
     * Returns a new reflection client using the supplied channel.
     */
    public static ServerReflectionClient create(Channel channel) {
        return new ServerReflectionClient(channel);
    }

    private ServerReflectionClient(Channel channel) {
        this.channel = channel;
    }

    /**
     * Asks the remote server to list its services and completes when the server responds.
     */
    public ListenableFuture<ImmutableList<String>> listServices() {
        ListServicesHandler rpcHandler = new ListServicesHandler();
        StreamObserver<ServerReflectionRequest> requestStream = ServerReflectionGrpc.newStub(channel)
                .withDeadlineAfter(LIST_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

    /**
     * Returns a {@link FileDescriptorSet} containing all the transitive dependencies of the supplied
     * service, as provided by the remote server.
     */
    public ListenableFuture<FileDescriptorSet> lookupService(String serviceName) {
        LookupServiceHandler rpcHandler = new LookupServiceHandler(serviceName);
        StreamObserver<ServerReflectionRequest> requestStream = ServerReflectionGrpc.newStub(channel)
                .withDeadlineAfter(LOOKUP_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

    /**
     * Handles the rpc life cycle of a single list operation.
     */
    private static class ListServicesHandler implements StreamObserver<ServerReflectionResponse> {
        private final SettableFuture<ImmutableList<String>> resultFuture;
        private StreamObserver<ServerReflectionRequest> requestStream;

        private ListServicesHandler() {
            resultFuture = SettableFuture.create();
        }

        ListenableFuture<ImmutableList<String>> start(
                StreamObserver<ServerReflectionRequest> requestStream) {
            this.requestStream = requestStream;
            requestStream.onNext(LIST_SERVICES_REQUEST);
            return resultFuture;
        }

        @Override
        public void onNext(ServerReflectionResponse serverReflectionResponse) {
            MessageResponseCase responseCase = serverReflectionResponse.getMessageResponseCase();
            switch (responseCase) {
                case LIST_SERVICES_RESPONSE:
                    handleListServiceResponse(serverReflectionResponse.getListServicesResponse());
                    break;
                default:
                    logger.warn("Got unknown reflection response type: " + responseCase);
                    break;
            }
        }

        @Override
        public void onError(Throwable t) {
            resultFuture.setException(new RuntimeException("Error in server reflection rpc while listing services", t));
        }

        @Override
        public void onCompleted() {
            if (!resultFuture.isDone()) {
                logger.error("Unexpected completion of server reflection rpc while listing services");
                resultFuture.setException(new RuntimeException("Unexpected end of rpc"));
            }
        }

        private void handleListServiceResponse(ListServiceResponse response) {
            ImmutableList.Builder<String> servicesBuilder = ImmutableList.builder();
            response.getServiceList().forEach(service -> servicesBuilder.add(service.getName()));
            resultFuture.set(servicesBuilder.build());
            requestStream.onCompleted();
        }
    }

    /**
     * Handles the rpc life cycle of a single lookup operation.
     */
    private static class LookupServiceHandler implements StreamObserver<ServerReflectionResponse> {
        private final SettableFuture<FileDescriptorSet> resultFuture;
        private final String serviceName;
        private final HashSet<String> requestedDescriptors;
        private final HashMap<String, FileDescriptorProto> resolvedDescriptors;
        private StreamObserver<ServerReflectionRequest> requestStream;

        // Used to notice when we've received all the files we've asked for and we can end the rpc.
        private int outstandingRequests;

        private LookupServiceHandler(String serviceName) {
            this.serviceName = serviceName;
            this.resultFuture = SettableFuture.create();
            this.resolvedDescriptors = new HashMap<>();
            this.requestedDescriptors = new HashSet<>();
            this.outstandingRequests = 0;
        }

        ListenableFuture<FileDescriptorSet> start(
                StreamObserver<ServerReflectionRequest> requestStream) {
            this.requestStream = requestStream;
            requestStream.onNext(requestForSymbol(serviceName));
            ++outstandingRequests;
            return resultFuture;
        }

        @Override
        public void onNext(ServerReflectionResponse response) {
            MessageResponseCase responseCase = response.getMessageResponseCase();
            switch (responseCase) {
                case FILE_DESCRIPTOR_RESPONSE:
                    ImmutableSet<FileDescriptorProto> descriptors =
                            parseDescriptors(response.getFileDescriptorResponse().getFileDescriptorProtoList());
                    descriptors.forEach(d -> resolvedDescriptors.put(d.getName(), d));
                    descriptors.forEach(this::processDependencies);
                    break;
                default:
                    logger.warn("Got unknown reflection response type: " + responseCase);
                    break;
            }
        }

        @Override
        public void onError(Throwable t) {
            resultFuture.setException(new RuntimeException("Reflection lookup rpc failed for: " + serviceName, t));
        }

        @Override
        public void onCompleted() {
            if (!resultFuture.isDone()) {
                logger.error("Unexpected completion of the server reflection rpc");
                resultFuture.setException(new RuntimeException("Unexpected end of rpc"));
            }
        }

        private ImmutableSet<FileDescriptorProto> parseDescriptors(List<ByteString> descriptorBytes) {
            ImmutableSet.Builder<FileDescriptorProto> resultBuilder = ImmutableSet.builder();
            for (ByteString fileDescriptorBytes : descriptorBytes) {
                try {
                    resultBuilder.add(FileDescriptorProto.parseFrom(fileDescriptorBytes));
                } catch (InvalidProtocolBufferException e) {
                    logger.warn("Failed to parse bytes as file descriptor proto");
                }
            }
            return resultBuilder.build();
        }

        private void processDependencies(FileDescriptorProto fileDescriptor) {
            logger.debug("Processing deps of descriptor: " + fileDescriptor.getName());
            fileDescriptor.getDependencyList().forEach(dep -> {
                if (!resolvedDescriptors.containsKey(dep) && !requestedDescriptors.contains(dep)) {
                    requestedDescriptors.add(dep);
                    ++outstandingRequests;
                    requestStream.onNext(requestForDescriptor(dep));
                }
            });

            --outstandingRequests;
            if (outstandingRequests == 0) {
                logger.debug("Retrieved service definition for [{}] by reflection", serviceName);
                resultFuture.set(FileDescriptorSet.newBuilder()
                        .addAllFile(resolvedDescriptors.values())
                        .build());
                requestStream.onCompleted();
            }
        }

        private static ServerReflectionRequest requestForDescriptor(String name) {
            return ServerReflectionRequest.newBuilder()
                    .setFileByFilename(name)
                    .build();
        }

        private static ServerReflectionRequest requestForSymbol(String symbol) {
            return ServerReflectionRequest.newBuilder()
                    .setFileContainingSymbol(symbol)
                    .build();
        }
    }
}
