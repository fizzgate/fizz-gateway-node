package we.service_registry.eureka;

import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;

public class FizzEurekaServiceRegistration {

    public String id;

    public EurekaRegistration registration;

    public EurekaServiceRegistry serviceRegistry;

    public CloudEurekaClient client;
}
