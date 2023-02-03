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

package com.fizzgate.service_registry.eureka;

import com.fizzgate.service_registry.FizzServiceRegistration;
import com.fizzgate.util.Consts;
import com.fizzgate.util.Utils;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author hongqiaowei
 */

public class FizzEurekaServiceRegistration extends FizzServiceRegistration {

    private final CloudEurekaClient client;

    private final long              renewalInterval;

    private       long              prevHeartbeatTimestamp = -1;

    public FizzEurekaServiceRegistration(String id, EurekaRegistration registration, EurekaServiceRegistry serviceRegistry, CloudEurekaClient client) {
        super(id, Type.EUREKA, registration, serviceRegistry);
        this.client = client;
        renewalInterval = this.client.getApplicationInfoManager().getInfo().getLeaseInfo().getRenewalIntervalInSecs() * 1000L;
    }

    public DiscoveryClient getDiscoveryClient() {
        return client;
    }

    @Override
    protected void shutdownClient() {
        client.shutdown();
        LOGGER.info("shutdown {} client", id);
    }

    @Override
    public ServerStatus getServerStatus() {
        EurekaClientConfig eurekaClientConfig = client.getEurekaClientConfig();
        List<String> eurekaServerServiceUrls = eurekaClientConfig.getEurekaServerServiceUrls(EurekaClientConfigBean.DEFAULT_ZONE);
        Map<String, Integer> registryCenterVip2port = new HashMap<>();
        for (String serviceUrl : eurekaServerServiceUrls) {
            String vip;
            int port;
            int at = serviceUrl.indexOf('@');
            if (at > -1) {
                int colon = serviceUrl.indexOf(':', at);
                if (colon > -1) {
                    int slash = serviceUrl.indexOf('/', colon);
                    vip = serviceUrl.substring(at + 1, colon);
                    port = Integer.parseInt(serviceUrl.substring(colon + 1, slash));
                } else {
                    int slash = serviceUrl.indexOf('/', at);
                    vip = serviceUrl.substring(at + 1, slash);
                    port = 80;
                }
            } else {
                int begin = serviceUrl.indexOf('/') + 2;
                int colon = serviceUrl.indexOf(':', begin);
                if (colon > -1) {
                    int slash = serviceUrl.indexOf('/', colon);
                    vip = serviceUrl.substring(begin, colon);
                    port = Integer.parseInt(serviceUrl.substring(colon + 1, slash));
                } else {
                    int slash = serviceUrl.indexOf('/', begin);
                    vip = serviceUrl.substring(begin, slash);
                    port = 80;
                }
            }
            registryCenterVip2port.put(vip, port);
        }

        boolean f = false;
        for (Application registeredApplication : client.getApplications().getRegisteredApplications()) {
            List<InstanceInfo> instances = registeredApplication.getInstances();
            for (InstanceInfo instance : instances) {
                String vipAddress = instance.getVIPAddress();
                String ipAddr = instance.getIPAddr();
                Integer port = registryCenterVip2port.get(vipAddress);
                if (port == null) {
                    port = registryCenterVip2port.get(ipAddr);
                }
                if (port != null) {
                    int p = instance.getPort();
                    if (p == port) {
                        f = true;
                        break;
                    }
                }
            }
            if (f) {
                for (InstanceInfo instance : instances) {
                    InstanceInfo.InstanceStatus status = instance.getStatus();
                    if (status != InstanceInfo.InstanceStatus.UP) {
                        return transfrom(status);
                    }
                }
                return transfrom(InstanceInfo.InstanceStatus.UP);
            }
        }

        long heartbeatTimestamp = client.getStats().lastSuccessfulHeartbeatTimestampMs();
        if (heartbeatTimestamp == -1) {
            return transfrom(InstanceInfo.InstanceStatus.STARTING);
        }
        if (heartbeatTimestamp > prevHeartbeatTimestamp) {
            prevHeartbeatTimestamp = heartbeatTimestamp;
            return transfrom(InstanceInfo.InstanceStatus.UP);
        }
        long duration = prevHeartbeatTimestamp + renewalInterval;
        if (System.currentTimeMillis() > duration) {
            LOGGER.warn("unknown eureka {} status", getId());
            return transfrom(InstanceInfo.InstanceStatus.UNKNOWN);
        } else {
            return transfrom(InstanceInfo.InstanceStatus.UP);
        }
        // String join = StringUtils.join(eurekaServerServiceUrls, ',');
        // throw Utils.runtimeExceptionWithoutStack("can't get eureka server instance status by " + join);
    }

    private ServerStatus transfrom(InstanceInfo.InstanceStatus status) {
        if (       status == InstanceInfo.InstanceStatus.UP) {
            return ServerStatus.UP;

        } else if (status == InstanceInfo.InstanceStatus.DOWN) {
            return ServerStatus.DOWN;

        } else if (status == InstanceInfo.InstanceStatus.OUT_OF_SERVICE) {
            return ServerStatus.OUT_OF_SERVICE;

        } else if (status == InstanceInfo.InstanceStatus.STARTING) {
            return ServerStatus.STARTING;

        } else {
            return ServerStatus.UNKNOWN;
        }
    }

    @Override
    public List<String> getServices() {
        List<Application> registeredApplications = client.getApplications().getRegisteredApplications();
        if (registeredApplications.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<String> services = new ArrayList<>(registeredApplications.size());
            for (Application app : registeredApplications) {
                services.add(app.getName().toLowerCase());
            }
            return services;
        }
    }

    @Override
    public String getInstance(String service) {
        InstanceInfo inst = getInstanceInfo(service);
        return inst.getIPAddr() + Consts.S.COLON + inst.getPort();
    }

    public InstanceInfo getInstanceInfo(String service) {
        List<InstanceInfo> insts = client.getInstancesByVipAddress(service, false);
        if (CollectionUtils.isEmpty(insts)) {
            throw Utils.runtimeExceptionWithoutStack(getId() + " eureka no " + service);
        }
        Applications apps = client.getApplications();
        int index = (int) (apps.getNextIndex(service.toUpperCase(), false).incrementAndGet() % insts.size());
        return insts.get(index);
    }
}
