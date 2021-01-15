package we.fizz.input.extension.grpc;

import com.alibaba.fastjson.JSON;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Mono;
import we.constants.CommonConstants;
import we.fizz.input.*;
import we.fizz.input.extension.dubbo.DubboRPCResponse;
import we.proxy.grpc.GrpcGenericService;
import we.proxy.grpc.GrpcInterfaceDeclaration;

import java.util.HashMap;
import java.util.Map;

public class GrpcInput extends RPCInput implements IInput {
    static public InputType TYPE = new InputType("GRPC");
    @Override
    protected Mono<RPCResponse> getClientSpecFromContext(InputConfig aConfig, InputContext inputContext) {
        GrpcInputConfig config = (GrpcInputConfig) aConfig;

        int timeout = config.getTimeout() < 1 ? 3000 : config.getTimeout() > 10000 ? 10000 : config.getTimeout();
        Map<String, String> attachments = (Map<String, String>) request.get("attachments");
        ConfigurableApplicationContext applicationContext = this.getCurrentApplicationContext();
        String body = (String)request.get("body");
        String url = (String)request.get("url");

        GrpcGenericService proxy = applicationContext.getBean(GrpcGenericService.class);
        GrpcInterfaceDeclaration declaration = new GrpcInterfaceDeclaration();
        declaration.setEndpoint(url);
        declaration.setServiceName(config.getServiceName());
        declaration.setMethod(config.getMethod());
        declaration.setTimeout(timeout);
        HashMap<String, Object> contextAttachment = null;
        if (attachments == null){
            contextAttachment = new HashMap<String, Object>();
        } else  {
            contextAttachment = new HashMap<String, Object>(attachments);
        }
        if (inputContext.getStepContext() != null &&  inputContext.getStepContext().getTraceId() != null){
            contextAttachment.put(CommonConstants.HEADER_TRACE_ID, inputContext.getStepContext().getTraceId());
        }

        Mono<Object> proxyResponse = proxy.send(body, declaration, contextAttachment);
        return proxyResponse.flatMap(cr->{
            DubboRPCResponse response = new DubboRPCResponse();
            String responseStr = JSON.toJSONString(cr);
            response.setBodyMono(Mono.just(responseStr));
            return Mono.just(response);
        });
    }

    public static Class inputConfigClass (){
        return GrpcInputConfig.class;
    }
}
