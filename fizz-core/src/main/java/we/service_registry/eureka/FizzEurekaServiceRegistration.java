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

package we.service_registry.eureka;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.util.CollectionUtils;
import we.service_registry.FizzServiceRegistration;
import we.util.Consts;
import we.util.Utils;

import java.util.List;

/**
 * @author hongqiaowei
 */

public class FizzEurekaServiceRegistration extends FizzServiceRegistration {

    private final CloudEurekaClient client;

    public FizzEurekaServiceRegistration(String id, EurekaRegistration registration, EurekaServiceRegistry serviceRegistry, CloudEurekaClient client) {
        super(id, registration, serviceRegistry);
        this.client = client;
    }

    public DiscoveryClient getDiscoveryClient() {
        return client;
    }

    public InstanceInfo.InstanceStatus getRegistryCenterStatus() {
        EurekaClientConfig eurekaClientConfig = client.getEurekaClientConfig();
        List<String> eurekaServerServiceUrls = eurekaClientConfig.getEurekaServerServiceUrls(EurekaClientConfigBean.DEFAULT_ZONE);
        boolean f = false;
        for (String serviceUrl : eurekaServerServiceUrls) {
            String vip;
            int port;
            int begin = serviceUrl.indexOf('p') + 4;
            int colon = serviceUrl.indexOf(':', begin);
            if (colon > -1) {
                int end = serviceUrl.indexOf('/', colon);
                vip = serviceUrl.substring(begin, colon);
                port = Integer.parseInt(serviceUrl.substring(colon + 1, end));
            } else {
                int end = serviceUrl.indexOf('/', begin);
                vip = serviceUrl.substring(begin, end);
                port = 80;
            }

            Applications applications = client.getApplications(serviceUrl);
            for (Application registeredApplication : applications.getRegisteredApplications()) {
                List<InstanceInfo> instances = registeredApplication.getInstances();
                for (InstanceInfo instance : instances) {
                    String vipAddress = instance.getVIPAddress();
                    String ipAddr = instance.getIPAddr();
                    if (vipAddress.equals(vip) || ipAddr.equals(vip)) {
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
                            return status;
                        }
                    }
                    return InstanceInfo.InstanceStatus.UP;
                }
            }
        }

        String join = StringUtils.join(eurekaServerServiceUrls, ',');
        throw Utils.runtimeExceptionWithoutStack("can't find any server with " + join);
    }

    @Override
    public String getInstance(String service) {
        InstanceInfo inst = getInstanceInfo(service);
        return inst.getIPAddr() + Consts.S.COLON + inst.getPort();
    }

    public InstanceInfo getInstanceInfo(String service) {
        List<InstanceInfo> insts = client.getInstancesByVipAddress(service, false);
        if (CollectionUtils.isEmpty(insts)) {
            throw Utils.runtimeExceptionWithoutStack(id + " eureka no " + service);
        }
        Applications apps = client.getApplications();
        int index = (int) (apps.getNextIndex(service.toUpperCase(), false).incrementAndGet() % insts.size());
        return insts.get(index);
    }
}
