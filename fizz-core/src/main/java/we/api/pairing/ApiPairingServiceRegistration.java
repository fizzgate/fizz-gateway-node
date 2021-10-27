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

package we.api.pairing;

import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.nacos.api.naming.NamingService;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.cloud.netflix.eureka.*;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import we.config.SystemConfig;
import we.service_registry.nacos.FizzNacosDiscoveryProperties;
import we.util.ReflectionUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author hongqiaowei
 */

@Configuration
@ConditionalOnDiscoveryEnabled
@ConditionalOnProperty(name = SystemConfig.FIZZ_API_PAIRING_CLIENT_ENABLE, havingValue = "true")
@AutoConfigureAfter({EurekaClientAutoConfiguration.class, NacosDiscoveryAutoConfiguration.class})
public class ApiPairingServiceRegistration<R extends Registration> implements ApplicationListener<FizzApiPairingWebServerInitializedEvent> {

    private ServiceRegistry serviceRegistry;

    private EurekaRegistration eurekaRegistration;

    private CloudEurekaClient eurekaClient;

    private final InetUtils inetUtils = new InetUtils(new InetUtilsProperties());

    @SneakyThrows
    @Override
    public void onApplicationEvent(FizzApiPairingWebServerInitializedEvent event) {

        ReactiveWebServerApplicationContext applicationContext = event.getApplicationContext();
        // ConfigurableEnvironment env = applicationContext.getEnvironment();


        /*FizzNacosDiscoveryProperties nacosDiscoveryProperties = new FizzNacosDiscoveryProperties();
        nacosDiscoveryProperties.setInetUtils(inetUtils);
        nacosDiscoveryProperties.setService("xxyyzz");
        nacosDiscoveryProperties.setIp("");
        nacosDiscoveryProperties.setPort(8601);
        nacosDiscoveryProperties.setGroup("DEFAULT_GROUP");
        nacosDiscoveryProperties.setClusterName("DEFAULT");
        nacosDiscoveryProperties.setNamespace("");
        nacosDiscoveryProperties.setSecretKey("");
        nacosDiscoveryProperties.setAccessKey("");
        nacosDiscoveryProperties.setUsername("");
        nacosDiscoveryProperties.setPassword("");
        nacosDiscoveryProperties.setEndpoint("");
        nacosDiscoveryProperties.setLogName("");
        nacosDiscoveryProperties.setNamingLoadCacheAtStart("false");

        nacosDiscoveryProperties.setServerAddr(":8848");

        Properties props = nacosDiscoveryProperties.getNacosProperties();
        props.put("enabled", true);
        props.put("server-addr", nacosDiscoveryProperties.getServerAddr());
        props.put("com.alibaba.nacos.naming.log.filename", "");

        nacosDiscoveryProperties.init();

        serviceRegistry = new NacosServiceRegistry(nacosDiscoveryProperties);
        NacosServiceManager nacosServiceManager = new NacosServiceManager();
        ReflectionUtils.set(serviceRegistry, "nacosServiceManager", nacosServiceManager);

        NacosRegistration nacosRegistration = new NacosRegistration(null, nacosDiscoveryProperties, applicationContext);
        serviceRegistry.register(nacosRegistration);

        NamingService namingService = nacosServiceManager.getNamingService(props);*/



        // eureka
        /*EurekaInstanceConfigBean eurekaInstanceConfig = new EurekaInstanceConfigBean(inetUtils);
        String app = "xxyyzz";
        eurekaInstanceConfig.setAppname(app);
        eurekaInstanceConfig.setVirtualHostName(app);
        eurekaInstanceConfig.setIpAddress("x.x.x.x");
        eurekaInstanceConfig.setNonSecurePort(8601);
        eurekaInstanceConfig.setInstanceId(eurekaInstanceConfig.getIpAddress() + ':' + app + ':' + eurekaInstanceConfig.getNonSecurePort());
        eurekaInstanceConfig.setPreferIpAddress(true);
        eurekaInstanceConfig.setSecurePortEnabled(false);
        eurekaInstanceConfig.setHealthCheckUrl("http://x.x.x.x:8601/actuator/info");
        eurekaInstanceConfig.setDataCenterInfo(new DataCenterInfo() {
            @Override
            public DataCenterInfo.Name getName() {
                return Name.MyOwn;
            }
        });

        InstanceInfo instanceInfo = new InstanceInfoFactory().create(eurekaInstanceConfig);

        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, instanceInfo);

        EurekaClientConfigBean eurekaClientConfig = new EurekaClientConfigBean();
        eurekaClientConfig.setRegion("default");
        Map<String, String> serviceUrlMap = new HashMap<>();
        serviceUrlMap.put(EurekaClientConfigBean.DEFAULT_ZONE, "http://x.x.x.x:6600/eureka/");
        eurekaClientConfig.setServiceUrl(serviceUrlMap);

        eurekaClient = new CloudEurekaClient(applicationInfoManager, eurekaClientConfig, null, applicationContext);

        SimpleObjectProvider<HealthCheckHandler> healthCheckHandler = new SimpleObjectProvider<>(null);
        eurekaRegistration = EurekaRegistration.builder(eurekaInstanceConfig).with(applicationInfoManager).with(healthCheckHandler).with(eurekaClient).build();

        serviceRegistry = new EurekaServiceRegistry();
        serviceRegistry.register(eurekaRegistration);*/
    }

    @PreDestroy
    public void stop() {
    }
}
