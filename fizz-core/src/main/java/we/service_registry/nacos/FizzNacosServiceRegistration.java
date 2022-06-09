/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.service_registry.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.client.naming.cache.ServiceInfoHolder;
import com.alibaba.nacos.client.naming.core.ServerListManager;
import com.alibaba.nacos.client.naming.core.ServiceInfoUpdateService;
import com.alibaba.nacos.client.naming.remote.NamingClientProxyDelegate;
import com.alibaba.nacos.client.naming.remote.gprc.NamingGrpcClientProxy;
import com.alibaba.nacos.client.naming.remote.gprc.redo.NamingGrpcRedoService;
import com.alibaba.nacos.client.naming.remote.http.NamingHttpClientProxy;
import com.alibaba.nacos.common.remote.client.Connection;
import com.alibaba.nacos.common.remote.client.RpcClientStatus;
import com.alibaba.nacos.common.remote.client.grpc.GrpcClient;
import com.alibaba.nacos.common.utils.ThreadUtils;
import org.springframework.util.StringUtils;
import we.service_registry.FizzServiceRegistration;
import we.util.Consts;
import we.util.ReflectionUtils;
import we.util.Utils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * @author hongqiaowei
 */

public class FizzNacosServiceRegistration extends FizzServiceRegistration {

    private NamingService namingService;

    private final String  group;

    private List<String>  clusters;

    private boolean       useGroup;

    private boolean       userCluster;

    public FizzNacosServiceRegistration(String id, NacosRegistration registration, NacosServiceRegistry serviceRegistry, NamingService namingService) {
        super(id, Type.NACOS, registration, serviceRegistry);
        this.namingService = namingService;
        NacosDiscoveryProperties discoveryProperties = registration.getNacosDiscoveryProperties();
        group = discoveryProperties.getGroup();
        if (StringUtils.hasText(group)) {
            useGroup = true;
        }
        String cluster = discoveryProperties.getClusterName();
        if (StringUtils.hasText(cluster)) {
            userCluster = true;
            clusters = Collections.singletonList(cluster);
        }
    }

    public NamingService getNamingService() {
        return namingService;
    }

    @Override
    public void close() {
        ServiceInfoHolder serviceInfoHolder = (ServiceInfoHolder) ReflectionUtils.get(namingService, "serviceInfoHolder");
        NamingClientProxyDelegate namingClientProxyDelegate = (NamingClientProxyDelegate) ReflectionUtils.get(namingService, "clientProxy");
        try {
            serviceInfoHolder.shutdown();
            ServiceInfoUpdateService serviceInfoUpdateService = (ServiceInfoUpdateService) ReflectionUtils.get(namingClientProxyDelegate, "serviceInfoUpdateService");
            serviceInfoUpdateService.shutdown();
            ServerListManager serverListManager = (ServerListManager) ReflectionUtils.get(namingClientProxyDelegate, "serverListManager");
            serverListManager.shutdown();
            NamingHttpClientProxy namingHttpClientProxy = (NamingHttpClientProxy) ReflectionUtils.get(namingClientProxyDelegate, "httpClientProxy");
            namingHttpClientProxy.shutdown();
            NamingGrpcClientProxy namingGrpcClientProxy = (NamingGrpcClientProxy) ReflectionUtils.get(namingClientProxyDelegate, "grpcClientProxy");

            GrpcClient grpcClient = (GrpcClient) ReflectionUtils.get(namingGrpcClientProxy, "rpcClient");

            AtomicReference<RpcClientStatus> rpcClientStatus = (AtomicReference<RpcClientStatus>) ReflectionUtils.get(grpcClient, "rpcClientStatus");
            rpcClientStatus.set(RpcClientStatus.SHUTDOWN);
            LOGGER.info("shutdown {} grpc client ,set status to shutdown", getId());

            ScheduledExecutorService clientEventExecutor = (ScheduledExecutorService) ReflectionUtils.get(grpcClient, "clientEventExecutor");
            clientEventExecutor.shutdownNow();
            LOGGER.info("shutdown {} client event executor {}", getId(), clientEventExecutor);

            Connection currentConnection = (Connection) ReflectionUtils.get(grpcClient, "currentConnection");
            if (currentConnection != null) {
                ReflectionUtils.invokeMethod("closeConnection", grpcClient, currentConnection);
                LOGGER.info("close {} current connection {}", getId(), currentConnection.getConnectionId());
            }

            NamingGrpcRedoService namingGrpcRedoService = (NamingGrpcRedoService) ReflectionUtils.get(namingGrpcClientProxy, "redoService");
            namingGrpcRedoService.shutdown();

            ScheduledExecutorService scheduledExecutorService = (ScheduledExecutorService) ReflectionUtils.get(namingClientProxyDelegate, "executorService");
            ThreadUtils.shutdownThreadPool(scheduledExecutorService, NAMING_LOGGER);

            LOGGER.info("nacos {} client resource is closed", getId());

        } catch (Exception e) {
            LOGGER.error("nacos {} naming service shutdown exception", getId(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void shutdownClient() {
        /*try {
            namingService.shutDown();
        } catch (NacosException e) {
            LOGGER.error("nacos {} naming service shutdown exception", getId(), e);
            throw new RuntimeException(e);
        }*/
    }

    @Override
    public ServerStatus getServerStatus() {
        String status = namingService.getServerStatus();
        return transfrom(status);
    }

    private ServerStatus transfrom(String status) {
        if (status.equals("UP")) {
            return ServerStatus.UP;

        } else if (status.equals("DOWN")) {
            return ServerStatus.DOWN;

        } else {
            LOGGER.warn("nacos {} status is {}", getId(), status);
            return ServerStatus.UNKNOWN;
        }
    }

    @Override
    public List<String> getServices() {
        try {
            ListView<String> servicesOfServer;
            if (useGroup) {
                servicesOfServer = namingService.getServicesOfServer(1, Integer.MAX_VALUE, group);
            } else {
                servicesOfServer = namingService.getServicesOfServer(1, Integer.MAX_VALUE);
            }
            return servicesOfServer.getData();
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getInstance(String service) {
        Instance instance = getInstanceInfo(service);
        return instance.getIp() + Consts.S.COLON + instance.getPort();
    }

    public Instance getInstanceInfo(String service) {
        Instance instance = null;
        try {
            if (useGroup && userCluster) {
                instance = namingService.selectOneHealthyInstance(service, group, clusters);
            } else if (useGroup) {
                instance = namingService.selectOneHealthyInstance(service, group);
            } else if (userCluster) {
                instance = namingService.selectOneHealthyInstance(service, clusters);
            } else {
                instance = namingService.selectOneHealthyInstance(service);
            }
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        if (instance == null) {
            throw Utils.runtimeExceptionWithoutStack(getId() + " nacos no " + service);
        }
        return instance;
    }
}
