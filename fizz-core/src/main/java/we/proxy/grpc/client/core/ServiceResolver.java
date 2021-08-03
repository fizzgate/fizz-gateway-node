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

package we.proxy.grpc.client.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A locator used to read proto file descriptors and extract method definitions.
 */
public class ServiceResolver {
    private static final Logger logger = LoggerFactory.getLogger(ServiceResolver.class);
    private final ImmutableList<FileDescriptor> fileDescriptors;

    /**
     * Creates a resolver which searches the supplied {@link FileDescriptorSet}.
     */
    public static ServiceResolver fromFileDescriptorSet(FileDescriptorSet descriptorSet) {
        ImmutableMap<String, FileDescriptorProto> descriptorProtoIndex =
                computeDescriptorProtoIndex(descriptorSet);
        Map<String, FileDescriptor> descriptorCache = new HashMap<>();

        ImmutableList.Builder<FileDescriptor> result = ImmutableList.builder();
        for (FileDescriptorProto descriptorProto : descriptorSet.getFileList()) {
            try {
                result.add(descriptorFromProto(descriptorProto, descriptorProtoIndex, descriptorCache));
            } catch (DescriptorValidationException e) {
                logger.warn("Skipped descriptor " + descriptorProto.getName() + " due to error", e);
            }
        }
        return new ServiceResolver(result.build());
    }

    private ServiceResolver(Iterable<FileDescriptor> fileDescriptors) {
        this.fileDescriptors = ImmutableList.copyOf(fileDescriptors);
    }

    /**
     * Lists all of the services found in the file descriptors
     */
    public Iterable<ServiceDescriptor> listServices() {
        ArrayList<ServiceDescriptor> serviceDescriptors = new ArrayList<ServiceDescriptor>();
        for (FileDescriptor fileDescriptor : fileDescriptors) {
            serviceDescriptors.addAll(fileDescriptor.getServices());
        }
        return serviceDescriptors;
    }

    /**
     * Lists all the known message types.
     */
    public ImmutableSet<Descriptor> listMessageTypes() {
        ImmutableSet.Builder<Descriptor> resultBuilder = ImmutableSet.builder();
        fileDescriptors.forEach(d -> resultBuilder.addAll(d.getMessageTypes()));
        return resultBuilder.build();
    }

    /**
     * Returns the descriptor of a protobuf method with the supplied grpc method name. If the method
     * cannot be found, this throws {@link IllegalArgumentException}.
     */
    public MethodDescriptor resolveServiceMethod(GrpcMethodDefinition definition) {

        ServiceDescriptor service = findService(definition.getPackageName(), definition.getServiceName());
        MethodDescriptor method = service.findMethodByName(definition.getMethodName());
        if (method == null) {
            throw new IllegalArgumentException(
                    "Unable to find method " + definition.getMethodName()
                            + " in service " + definition.getServiceName());
        }
        return method;
    }

    private ServiceDescriptor findService(String packageName, String serviceName) {
        // TODO(dino): Consider creating an index.
        for (FileDescriptor fileDescriptor : fileDescriptors) {
            if (!fileDescriptor.getPackage().equals(packageName)) {
                // Package does not match this file, ignore.
                continue;
            }

            ServiceDescriptor serviceDescriptor = fileDescriptor.findServiceByName(serviceName);
            if (serviceDescriptor != null) {
                return serviceDescriptor;
            }
        }
        throw new IllegalArgumentException("Unable to find service with name: " + serviceName);
    }

    /**
     * Returns a map from descriptor proto name as found inside the descriptors to protos.
     */
    private static ImmutableMap<String, FileDescriptorProto> computeDescriptorProtoIndex(
            FileDescriptorSet fileDescriptorSet) {
        ImmutableMap.Builder<String, FileDescriptorProto> resultBuilder = ImmutableMap.builder();
        for (FileDescriptorProto descriptorProto : fileDescriptorSet.getFileList()) {
            resultBuilder.put(descriptorProto.getName(), descriptorProto);
        }
        return resultBuilder.build();
    }

    /**
     * Recursively constructs file descriptors for all dependencies of the supplied proto and returns
     * a {@link FileDescriptor} for the supplied proto itself. For maximal efficiency, reuse the
     * descriptorCache argument across calls.
     */
    private static FileDescriptor descriptorFromProto(
            FileDescriptorProto descriptorProto,
            ImmutableMap<String, FileDescriptorProto> descriptorProtoIndex,
            Map<String, FileDescriptor> descriptorCache) throws DescriptorValidationException {
        // First, check the cache.
        String descriptorName = descriptorProto.getName();
        if (descriptorCache.containsKey(descriptorName)) {
            return descriptorCache.get(descriptorName);
        }

        // Then, fetch all the required dependencies recursively.
        ImmutableList.Builder<FileDescriptor> dependencies = ImmutableList.builder();
        for (String dependencyName : descriptorProto.getDependencyList()) {
            if (!descriptorProtoIndex.containsKey(dependencyName)) {
                throw new IllegalArgumentException("Could not find dependency: " + dependencyName);
            }
            FileDescriptorProto dependencyProto = descriptorProtoIndex.get(dependencyName);
            dependencies.add(descriptorFromProto(dependencyProto, descriptorProtoIndex, descriptorCache));
        }

        // Finally, construct the actual descriptor.
        FileDescriptor[] empty = new FileDescriptor[0];
        return FileDescriptor.buildFrom(descriptorProto, dependencies.build().toArray(empty));
    }

    public List<FileDescriptor> getFileDescriptors() {
        return fileDescriptors;
    }
}
