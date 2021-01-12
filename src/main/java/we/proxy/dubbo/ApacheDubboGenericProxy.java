package we.proxy.dubbo;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import we.fizz.exception.FizzException;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class ApacheDubboGenericProxy {

    @PostConstruct
    public void afterPropertiesSet() {

    }

    public ReferenceConfig<GenericService> createReferenceConfig(String serviceName){
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("fizz_proxy");
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://127.0.0.1:2181");
        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(serviceName);
        applicationConfig.setRegistry(registryConfig);
        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setGeneric(true);
        referenceConfig.setAsync(true);
        referenceConfig.setTimeout(7000);
        return referenceConfig;
    }

    /**
     * Generic invoke.
     *
     * @param body     the json string body
     * @param interfaceDeclaration the interface declaration
     * @return the object
     * @throws FizzException the fizz exception
     */
    public Mono<Object> send(final String body, final DubboInterfaceDeclaration interfaceDeclaration, HashMap<String, String> attachments ) {

        RpcContext.getContext().setAttachments(attachments);
        ReferenceConfig<GenericService> reference = createReferenceConfig(interfaceDeclaration.getServiceName());
        reference.setTimeout(interfaceDeclaration.getTimeout());
        GenericService genericService = reference.get();
        Pair<String[], Object[]> pair;
        if (DubboUtils.isEmpty(body)) {
            pair = new ImmutablePair<String[], Object[]>(new String[]{}, new Object[]{});
        } else {
            pair = DubboUtils.parseDubboParam(body, interfaceDeclaration.getParameterTypes());

        }
        CompletableFuture<Object> future = genericService.$invokeAsync(interfaceDeclaration.getMethod(), pair.getLeft(), pair.getRight());
        return Mono.fromFuture(future.thenApply(ret -> {
            return ret;
        })).onErrorMap(exception -> exception instanceof GenericException ? new FizzException(((GenericException) exception).getExceptionMessage()) : new FizzException(exception));
    }

}
