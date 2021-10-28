package we.service_registry.eureka;

import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;

public class FizzEurekaServiceRegistration {

    public String id;

    public EurekaRegistration registration;

    public EurekaServiceRegistry serviceRegistry;

    public CloudEurekaClient client;

    public FizzEurekaServiceRegistration(String id, EurekaRegistration registration, EurekaServiceRegistry serviceRegistry, CloudEurekaClient client) {
        this.id = id;
        this.registration = registration;
        this.serviceRegistry = serviceRegistry;
        this.client = client;
    }
}
