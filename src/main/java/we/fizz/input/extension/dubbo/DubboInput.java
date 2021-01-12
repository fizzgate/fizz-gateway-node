package we.fizz.input.extension.dubbo;

import com.alibaba.fastjson.JSON;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Mono;
import we.constants.CommonConstants;
import we.fizz.input.*;
import we.proxy.dubbo.ApacheDubboGenericProxy;
import we.proxy.dubbo.DubboInterfaceDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author linwaiwai
 *
 */
public class DubboInput extends RPCInput {
    static public InputType TYPE = new InputType("DUBBO");
    private static final Logger LOGGER = LoggerFactory.getLogger(DubboInput.class);

    @Override
    protected Mono<RPCResponse> getClientSpecFromContext(InputConfig aConfig, InputContext inputContext) {
        DubboInputConfig config = (DubboInputConfig) aConfig;

        int timeout = config.getTimeout() < 1 ? 3000 : config.getTimeout() > 10000 ? 10000 : config.getTimeout();
        Map<String, String> attachments = (Map<String, String>) request.get("attachments");
        ConfigurableApplicationContext applicationContext = this.getCurrentApplicationContext();
        String body = (String)request.get("body");

        ApacheDubboGenericProxy proxy = applicationContext.getBean(ApacheDubboGenericProxy.class);
        DubboInterfaceDeclaration declaration = new DubboInterfaceDeclaration();
        declaration.setServiceName(config.getServiceName());
        declaration.setMethod(config.getMethod());
        declaration.setParameterTypes(config.getParameterTypes());
        declaration.setTimeout(timeout);

        HashMap<String, String> contextAttachment = new HashMap<String, String>(attachments);
        contextAttachment.put(CommonConstants.HEADER_TRACE_ID, inputContext.getStepContext().getTraceId());
        return proxy.send(body, declaration, contextAttachment).flatMap(cr->{
            DubboRPCResponse response = new DubboRPCResponse();
            String responseStr = JSON.toJSONString(cr);
            response.setBodyMono(Mono.just(responseStr));
            return Mono.just(response);
        });
    }

    protected void doRequestMapping(InputConfig aConfig, InputContext inputContext) {

    }

    protected void doOnResponseSuccess(RPCResponse cr, long elapsedMillis) {

    }
    protected Mono<String> bodyToMono(RPCResponse cr){
        return cr.getBodyMono();
    }

    protected void doOnBodyError(Throwable ex, long elapsedMillis) {

    }

    protected void doOnBodySuccess(String resp, long elapsedMillis) {

    }

    protected void doResponseMapping(InputConfig aConfig, InputContext inputContext, String responseBody) {
    }

    public static Class inputConfigClass (){
        return DubboInputConfig.class;
    }


}
