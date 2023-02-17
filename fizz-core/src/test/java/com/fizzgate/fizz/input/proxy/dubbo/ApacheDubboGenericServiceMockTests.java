package com.fizzgate.fizz.input.proxy.dubbo;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.service.GenericService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fizzgate.fizz.group.FastTestGroup;
import com.fizzgate.proxy.dubbo.ApacheDubboGenericService;
import com.fizzgate.proxy.dubbo.DubboInterfaceDeclaration;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
@Category(FastTestGroup.class)
public class ApacheDubboGenericServiceMockTests {
    private static final String SERVICE_NAME = "com.fizzgate.test";
    private static final String METHOD_NAME = "method";
    private static final String[] LEFT = new String[]{};

    private static final Object[] RIGHT = new Object[]{};
    @Before
    public void setup(){

    }

    public ApacheDubboGenericService getMockApachDubbo(){
        ReferenceConfig referenceConfig = mock(ReferenceConfig.class);
        GenericService genericService = mock(GenericService.class);
        when(referenceConfig.get()).thenReturn(genericService);
        when(referenceConfig.getInterface()).thenReturn(SERVICE_NAME);
        ApacheDubboGenericService apacheDubboProxyService = mock(ApacheDubboGenericService.class);
        when(apacheDubboProxyService.createReferenceConfig(SERVICE_NAME, null, null)).thenReturn(referenceConfig);
        CompletableFuture<Object> future = new CompletableFuture<>();
        when(genericService.$invokeAsync(METHOD_NAME, LEFT, RIGHT)).thenReturn(future);
        future.complete("success");
        return apacheDubboProxyService;
    }
    @Test
    public void test() {
        HashMap<String, String> attachments = mock(HashMap.class);
        DubboInterfaceDeclaration declaration =  mock(DubboInterfaceDeclaration.class);
        declaration.setServiceName(SERVICE_NAME);
        declaration.setMethod(METHOD_NAME);
        declaration.setParameterTypes("java.lang.String, java.lang.String");
        declaration.setTimeout(3000);
        ApacheDubboGenericServiceMockTests test = new ApacheDubboGenericServiceMockTests();
        ApacheDubboGenericService apacheDubboProxyService = test.getMockApachDubbo();
        apacheDubboProxyService.send(null, declaration, attachments);
    }

}
