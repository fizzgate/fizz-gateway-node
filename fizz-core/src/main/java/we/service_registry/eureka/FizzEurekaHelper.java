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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public abstract class FizzEurekaHelper {

    public static FizzEurekaServiceRegistration getServiceRegistration(FizzEurekaProperties fizzEurekaProperties) {

        InetUtils inetUtils = fizzEurekaProperties.applicationContext.getBean(InetUtils.class);
        EurekaInstanceConfigBean eurekaInstanceConfig = new EurekaInstanceConfigBean(inetUtils);
        eurekaInstanceConfig.setAppname(fizzEurekaProperties.appName);
        eurekaInstanceConfig.setVirtualHostName(fizzEurekaProperties.getVirtualHostName());
        eurekaInstanceConfig.setNonSecurePort(fizzEurekaProperties.nonSecurePort);

        String ip = fizzEurekaProperties.ipAddress;
        String instanceId;
        if (ip == null) {
            ip = inetUtils.findFirstNonLoopbackAddress().getHostAddress();
            instanceId = ip + ':' + fizzEurekaProperties.appName + ':' + fizzEurekaProperties.nonSecurePort;
        } else {
            instanceId = ip + ':' + fizzEurekaProperties.appName + ':' + fizzEurekaProperties.nonSecurePort;
            fizzEurekaProperties.instanceId(instanceId);
        }
        eurekaInstanceConfig.setIpAddress(ip);

        eurekaInstanceConfig.setInstanceId(instanceId);
        eurekaInstanceConfig.setPreferIpAddress(fizzEurekaProperties.preferIpAddress);
        eurekaInstanceConfig.setSecurePortEnabled(fizzEurekaProperties.securePortEnabled);
        String healthCheckUrl = fizzEurekaProperties.getHealthCheckUrl();
        if (healthCheckUrl != null) {
            eurekaInstanceConfig.setHealthCheckUrl(healthCheckUrl);
        }
        eurekaInstanceConfig.setDataCenterInfo(fizzEurekaProperties.dataCenterInfo);

        InstanceInfo instanceInfo = new InstanceInfoFactory().create(eurekaInstanceConfig);

        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, instanceInfo);

        EurekaClientConfigBean eurekaClientConfig = new EurekaClientConfigBean();
        eurekaClientConfig.setRegion(fizzEurekaProperties.region);
        Map<String, String> serviceUrlMap = new HashMap<>();
        serviceUrlMap.put(fizzEurekaProperties.zone, fizzEurekaProperties.serviceUrl);
        eurekaClientConfig.setServiceUrl(serviceUrlMap);

        CloudEurekaClient eurekaClient = new CloudEurekaClient(applicationInfoManager, eurekaClientConfig, null, fizzEurekaProperties.applicationContext);

        SimpleObjectProvider<HealthCheckHandler> healthCheckHandler = new SimpleObjectProvider<>(null);
        EurekaRegistration eurekaRegistration = EurekaRegistration.builder(eurekaInstanceConfig).with(applicationInfoManager).with(healthCheckHandler).with(eurekaClient).build();
        EurekaServiceRegistry serviceRegistry = new EurekaServiceRegistry();
        return new FizzEurekaServiceRegistration(fizzEurekaProperties.getId(), eurekaRegistration, serviceRegistry, eurekaClient);
    }

    public static Map<String, FizzEurekaServiceRegistration> getServiceRegistration(List<FizzEurekaProperties> fizzEurekaPropertiesList) {
        Map<String, FizzEurekaServiceRegistration> result = new HashMap<>();
        for (FizzEurekaProperties properties : fizzEurekaPropertiesList) {
            FizzEurekaServiceRegistration fizzEurekaServiceRegistration = getServiceRegistration(properties);
            result.put(properties.getId(), fizzEurekaServiceRegistration);
        }
        return result;
    }
}
