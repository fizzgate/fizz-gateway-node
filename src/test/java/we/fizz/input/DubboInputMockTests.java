package we.fizz.input;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ConfigurableApplicationContext;
import we.fizz.group.FastTestGroup;
import we.fizz.Step;
import we.fizz.StepContext;
import we.fizz.StepResponse;

import we.fizz.input.extension.dubbo.DubboInput;
import we.fizz.input.extension.dubbo.DubboInputConfig;
import we.proxy.dubbo.ApacheDubboGenericService;
import we.proxy.dubbo.DubboInterfaceDeclaration;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Category(FastTestGroup.class)
public class DubboInputMockTests {
    private static final String SERVICE_NAME = "com.fizzgate.test";
    private static final String METHOD_NAME = "method";
    private static final String[] LEFT = new String[]{};

    private static final Object[] RIGHT = new Object[]{};

    private ApacheDubboGenericService proxy;
//    @Before
//    public void setup(){
//        ApacheDubboGenericProxyTests test = new ApacheDubboGenericProxyTests();
//        proxy = test.getMockApachDubbo();
//    }

    @Test
    public void test() {
        DubboInterfaceDeclaration declaration = new DubboInterfaceDeclaration();
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
        ApacheDubboGenericService apacheDubboProxyService = new ApacheDubboGenericService();
        ApacheDubboGenericService proxy = spy(apacheDubboProxyService);
        when(proxy.createReferenceConfig(SERVICE_NAME, null, null)).thenReturn(referenceConfig);

        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        when(applicationContext.getBean(ApacheDubboGenericService.class)).thenReturn(proxy);

        Step step = mock(Step.class);
        when(step.getCurrentApplicationContext()).thenReturn(applicationContext);

        StepResponse stepResponse = new StepResponse(step, null, new HashMap<String, Map<String, Object>>());
        DubboInputConfig config = mock(DubboInputConfig.class);
        when(config.getServiceName()).thenReturn(SERVICE_NAME);
        InputFactory.registerInput(DubboInput.TYPE, DubboInput.class);
        DubboInput dubboInput = (DubboInput)InputFactory.createInput(DubboInput.TYPE.toString());

        dubboInput.setName("input1");
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
