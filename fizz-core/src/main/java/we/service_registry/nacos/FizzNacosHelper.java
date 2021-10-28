package we.service_registry.nacos;

import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.nacos.api.naming.NamingService;
import we.util.ReflectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FizzNacosHelper {

    public static FizzNacosServiceRegistration getServiceRegistration(FizzNacosProperties fizzNacosProperties) {
        fizzNacosProperties.init();
        NacosServiceRegistry nacosServiceRegistry = new NacosServiceRegistry(fizzNacosProperties);
        NacosServiceManager nacosServiceManager = new NacosServiceManager();
        ReflectionUtils.set(nacosServiceRegistry, "nacosServiceManager", nacosServiceManager);
        NacosRegistration nacosRegistration = new NacosRegistration(null, fizzNacosProperties, fizzNacosProperties.getApplicationContext());
        NamingService namingService = nacosServiceManager.getNamingService(fizzNacosProperties.getNacosProperties());
        return new FizzNacosServiceRegistration(fizzNacosProperties.getId(), nacosRegistration, nacosServiceRegistry, namingService);
    }

    public static Map<String, FizzNacosServiceRegistration> getServiceRegistration(List<FizzNacosProperties> fizzNacosPropertiesList) {
        Map<String, FizzNacosServiceRegistration> result = new HashMap<>();
        for (FizzNacosProperties properties : fizzNacosPropertiesList) {
            FizzNacosServiceRegistration fizzNacosServiceRegistration = getServiceRegistration(properties);
            result.put(properties.getId(), fizzNacosServiceRegistration);
        }
        return result;
    }
}
