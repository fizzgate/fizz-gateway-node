package we.service_registry.eureka;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract  class EurekaHelper {

    private static final InetUtils inetUtils = new InetUtils(new InetUtilsProperties());

    public static FizzEurekaServiceRegistration get(FizzEurekaProperties fizzEurekaProperties) {
        return null;
    }

    public static Map<String,  FizzEurekaServiceRegistration> get(List<FizzEurekaProperties> fizzEurekaPropertiesList) {
        Map<String,  FizzEurekaServiceRegistration> result = new HashMap<>();
        for (FizzEurekaProperties properties : fizzEurekaPropertiesList) {
            FizzEurekaServiceRegistration fizzEurekaServiceRegistration = get(properties);
            result.put(properties.id, fizzEurekaServiceRegistration);
        }
        return result;
    }
}
