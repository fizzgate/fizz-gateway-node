package we.service_registry.nacos;

import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.nacos.api.naming.NamingService;

public class FizzNacosServiceRegistration {

    public String id;

    public NacosRegistration registration;

    public NacosServiceRegistry serviceRegistry;

    public NamingService namingService;

    public FizzNacosServiceRegistration(String id, NacosRegistration registration, NacosServiceRegistry serviceRegistry, NamingService namingService) {
        this.id = id;
        this.registration = registration;
        this.serviceRegistry = serviceRegistry;
        this.namingService = namingService;
    }
}
