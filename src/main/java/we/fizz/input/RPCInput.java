package we.fizz.input;
import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import we.exception.ExecuteScriptException;
import we.fizz.StepContext;
import we.fizz.input.extension.request.RequestInputConfig;
import we.flume.clients.log4j2appender.LogService;
import we.util.JacksonUtils;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class RPCInput extends Input {
    protected static final Logger LOGGER = LoggerFactory.getLogger(RPCInput.class.getName());
    protected static final String FALLBACK_MODE_STOP = "stop";
    protected static final String FALLBACK_MODE_CONTINUE = "continue";
    
    protected Map<String, Object> request = new HashMap<>();
    protected Map<String, Object> response = new HashMap<>();

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

    @Override
    @SuppressWarnings("unchecked")
    public boolean needRun(StepContext<String, Object> stepContext) {
        Map<String, Object> condition = ((RequestInputConfig) config).getCondition();
        if (CollectionUtils.isEmpty(condition)) {
            // 没有配置condition，直接运行
            return Boolean.TRUE;
        }

        ONode ctxNode = PathMapping.toONode(stepContext);
        try {
            Boolean needRun = ScriptHelper.execute(condition, ctxNode, stepContext, Boolean.class);
            return needRun != null ? needRun : Boolean.TRUE;
        } catch (ScriptException e) {
            LogService.setBizId(inputContext.getStepContext().getTraceId());
            LOGGER.warn("execute script failed, {}", JacksonUtils.writeValueAsString(condition), e);
            throw new ExecuteScriptException(e, stepContext, condition);
        }
    }

    private String prefix;

    @Override
    public Mono<Map> run() {
        long t1 = System.currentTimeMillis();
        this.doRequestMapping(config, inputContext);
        inputContext.getStepContext().addElapsedTime(stepResponse.getStepName() + "-" + this.name + "-RequestMapping",
                System.currentTimeMillis() - t1);

        prefix = stepResponse.getStepName() + "-" + "调用接口";
        long start = System.currentTimeMillis();
        Mono<RPCResponse> rpcResponse = this.getClientSpecFromContext(config, inputContext);
        Mono<String> body = rpcResponse.flatMap(cr->{
            return Mono.just(cr).doOnError(throwable -> cleanup(cr));
        }).doOnSuccess(cr -> {
            long elapsedMillis = System.currentTimeMillis() - start;
            this.doOnResponseSuccess(cr, elapsedMillis);

        }).flatMap(cr -> { return this.bodyToMono(cr); }).doOnSuccess(resp -> {
            long elapsedMillis = System.currentTimeMillis() - start;
            this.doOnBodySuccess(resp, elapsedMillis);
        }).doOnError(ex -> {
            long elapsedMillis = System.currentTimeMillis() - start;
            this.doOnBodyError(ex, elapsedMillis);
        });

        // fallback handler
        InputConfig reqConfig = (InputConfig) config;
        if (reqConfig.getFallback() != null) {
            Map<String, String> fallback = reqConfig.getFallback();
            String mode = fallback.get("mode");
            if (FALLBACK_MODE_STOP.equals(mode)) {
                body = body.onErrorStop();
            } else if (FALLBACK_MODE_CONTINUE.equals(mode)) {
                body = body.onErrorResume(ex -> {
                    return Mono.just(fallback.get("defaultResult"));
                });
            } else {
                body = body.onErrorStop();
            }
        }

        return body.flatMap(item -> {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("data", item);
            result.put("request", this);

            long t3 = System.currentTimeMillis();
            this.doResponseMapping(config, inputContext, item);
            inputContext.getStepContext().addElapsedTime(
                    stepResponse.getStepName() + "-" + this.name + "-ResponseMapping", System.currentTimeMillis() - t3);

            return Mono.just(result);
        });
    }

    private void cleanup(RPCResponse clientResponse) {

    }

    protected Mono<RPCResponse> getClientSpecFromContext(InputConfig aConfig, InputContext inputContext) {
        return null;
    }

}
