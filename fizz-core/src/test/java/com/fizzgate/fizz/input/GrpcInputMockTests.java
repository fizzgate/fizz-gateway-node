package com.fizzgate.fizz.input;

import com.fizzgate.fizz.Step;
import com.fizzgate.fizz.StepContext;
import com.fizzgate.fizz.StepResponse;
import com.fizzgate.fizz.group.FastTestGroup;
import com.fizzgate.fizz.input.InputContext;
import com.fizzgate.fizz.input.InputFactory;
import com.fizzgate.fizz.input.extension.grpc.GrpcInput;
import com.fizzgate.fizz.input.extension.grpc.GrpcInputConfig;
import com.fizzgate.proxy.grpc.GrpcGenericService;
import com.fizzgate.proxy.grpc.GrpcInterfaceDeclaration;
import com.fizzgate.proxy.grpc.client.GrpcProxyClient;
import com.fizzgate.proxy.grpc.client.utils.ChannelFactory;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Category(FastTestGroup.class)
public class GrpcInputMockTests {
    private static final String URL ="localhost:8090";
    private static final String SERVICE_NAME = "com.fizzgate.test";
    private static final String METHOD_NAME = "method";
    private static final String[] LEFT = new String[]{};

    private static final Object[] RIGHT = new Object[]{};

    private GrpcGenericService proxy;
//    @Before
//    public void setup(){
//        ApacheDubboGenericProxyTests test = new ApacheDubboGenericProxyTests();
//        proxy = test.getMockApachDubbo();
//    }

    @Test
    public void test() {
        mockStatic(ChannelFactory.class);
        GrpcInterfaceDeclaration declaration =  new GrpcInterfaceDeclaration();
        declaration.setEndpoint(URL);
        declaration.setServiceName(SERVICE_NAME);
        declaration.setMethod(METHOD_NAME);
        declaration.setTimeout(3000);

        GrpcProxyClient grpcProxyClient = mock(GrpcProxyClient.class);

        ListenableFuture<?> future = new ListenableFuture<String>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                return "result";
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }

            @Override
            public void addListener(Runnable runnable, Executor executor) {

            }
        };
        when(grpcProxyClient.invokeMethodAsync(any(), any(), any(), any(), any())).thenReturn((ListenableFuture<Void>) future);

        GrpcGenericService grpcGenericService = new GrpcGenericService();
        ReflectionTestUtils.setField(grpcGenericService, "grpcProxyClient", grpcProxyClient);

        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        when(applicationContext.getBean(GrpcGenericService.class)).thenReturn(grpcGenericService);

        Step step = mock(Step.class);
        when(step.getCurrentApplicationContext()).thenReturn(applicationContext);

        StepResponse stepResponse = new StepResponse(step, null, new HashMap<String, Map<String, Object>>());
        GrpcInputConfig config = mock(GrpcInputConfig.class);
        when(config.getServiceName()).thenReturn(SERVICE_NAME);
        InputFactory.registerInput(GrpcInput.TYPE, GrpcInput.class);
        GrpcInput grpcInput = (GrpcInput)InputFactory.createInput(GrpcInput.TYPE.toString());
        HashMap <String, Object>request = new HashMap <String, Object>();
        request.put("url",URL);
        ReflectionTestUtils.setField(grpcInput, "request", request);
        grpcInput.setName("input1");
        grpcInput.setWeakStep(new SoftReference<>(step));
        grpcInput.setStepResponse(stepResponse);
        grpcInput.setConfig(config);
        StepContext stepContext = mock(StepContext.class);
        stepContext.put("step1", stepResponse);
        InputContext context = new InputContext(stepContext, null);
        grpcInput.beforeRun(context);

        grpcInput.run();


    }
}
