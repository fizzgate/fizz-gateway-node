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

public abstract class FizzEurekaHelper {

    public static FizzEurekaServiceRegistration getServiceRegistration(FizzEurekaProperties fizzEurekaProperties) {

        InetUtils inetUtils = fizzEurekaProperties.applicationContext.getBean(InetUtils.class);
        EurekaInstanceConfigBean eurekaInstanceConfig = new EurekaInstanceConfigBean(inetUtils);
        eurekaInstanceConfig.setAppname(fizzEurekaProperties.appName);
        eurekaInstanceConfig.setVirtualHostName(fizzEurekaProperties.getVirtualHostName());
        eurekaInstanceConfig.setIpAddress(fizzEurekaProperties.ipAddress);
        eurekaInstanceConfig.setNonSecurePort(fizzEurekaProperties.nonSecurePort);
        eurekaInstanceConfig.setInstanceId(fizzEurekaProperties.getInstanceId());
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
