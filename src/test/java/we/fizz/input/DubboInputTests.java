package we.fizz.input;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Mono;
import we.fizz.Step;
import we.fizz.StepContext;
import we.fizz.StepResponse;
import we.fizz.input.extension.dubbo.DubboInput;
import we.fizz.input.extension.dubbo.DubboInputConfig;
import we.fizz.input.proxy.dubbo.ApacheDubboGenericProxyTests;
import we.proxy.dubbo.ApacheDubboGenericProxy;
import we.proxy.dubbo.DubboInterfaceDeclaration;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DubboInputTests {
    private static final String SERVICE_NAME = "com.fizzgate.test";
    private static final String METHOD_NAME = "method";
    private static final String[] LEFT = new String[]{};

    private static final Object[] RIGHT = new Object[]{};

    private ApacheDubboGenericProxy proxy;
//    @Before
//    public void setup(){
//        ApacheDubboGenericProxyTests test = new ApacheDubboGenericProxyTests();
//        proxy = test.getMockApachDubbo();
//    }

    @Test
    public void test() {
        DubboInterfaceDeclaration declaration =  mock(DubboInterfaceDeclaration.class);
        declaration.setServiceName(SERVICE_NAME);
        declaration.setMethod(METHOD_NAME);
        declaration.setParameterTypes("java.lang.String, java.lang.String");
        declaration.setTimeout(3000);

        ReferenceConfig referenceConfig = mock(ReferenceConfig.class);
        GenericService genericService = mock(GenericService.class);
        when(referenceConfig.get()).thenReturn(genericService);
        when(referenceConfig.getInterface()).thenReturn(SERVICE_NAME);
        CompletableFuture<Object> future = new CompletableFuture<>();
        when(genericService.$invokeAsync(any(), any(), any())).thenReturn(future);
        ApacheDubboGenericProxy apacheDubboProxyService = new ApacheDubboGenericProxy();
        ApacheDubboGenericProxy proxy = spy(apacheDubboProxyService);
        when(proxy.createReferenceConfig(SERVICE_NAME)).thenReturn(referenceConfig);

        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        when(applicationContext.getBean(ApacheDubboGenericProxy.class)).thenReturn(proxy);

        Step step = mock(Step.class);
        when(step.getCurrentApplicationContext()).thenReturn(applicationContext);

        StepResponse stepResponse = new StepResponse(step, null, new HashMap<String, Map<String, Object>>());
        DubboInputConfig config = mock(DubboInputConfig.class);
        when(config.getServiceName()).thenReturn(SERVICE_NAME);
        InputFactory.registerInput(InputType.DUBBO, DubboInput.class);
        DubboInput dubboInput = (DubboInput)InputFactory.createInput(InputType.DUBBO.toString());

        dubboInput.setName("hello");
        dubboInput.setWeakStep(new SoftReference<>(step));
        dubboInput.setStepResponse(stepResponse);
        dubboInput.setConfig(config);
        StepContext stepContext = mock(StepContext.class);
        stepContext.put("step1", stepResponse);
        InputContext context = new InputContext(stepContext, null);
        dubboInput.beforeRun(context);

        dubboInput.run();

        future.complete("success");

    }
}
