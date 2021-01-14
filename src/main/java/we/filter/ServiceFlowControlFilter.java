package we.filter;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.stats.FlowStat;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.Constants;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;

@Component(ServiceFlowControlFilter.SERVICE_FLOW_CONTROL_FILTER)
@DependsOn(GlobalFlowControlFilter.GLOBAL_FLOW_CONTROL_FILTER)
@Order(-3)
public class ServiceFlowControlFilter extends ProxyAggrFilter {

    private static final Logger log                 = LoggerFactory.getLogger(ServiceFlowControlFilter.class);

    public  static final String SERVICE_FLOW_CONTROL_FILTER = "serviceFlowControlFilter";


    private static final String exceed              = " exceed ";
    private static final String concurrents         = " concurrents ";
    private static final String orQps               = " or qps ";

    @NacosValue(value = "${flowControl:false}", autoRefreshed = true)
    @Value("${flowControl:false}")
    private boolean flowControl;

    private FlowStat flowStat = GlobalFlowControlFilter.flowStat;

    @Resource
    private ResourceRateLimitConfigService resourceRateLimitConfigService;

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
        if (flowControl) {
            long currentTimeSlot = exchange.getAttribute("currentTimeSlot");
            String service = WebUtils.getClientService(exchange);
            ResourceRateLimitConfig config = resourceRateLimitConfigService.getResourceRateLimitConfig(service);

            if (config == null) {
                config = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.SERVICE_DEFAULT);
            }
            if (config == null || !config.isEnable()) {
                // 无服务流控配置
                flowStat.incrRequest(service, currentTimeSlot, null, null);
            } else {
                boolean concurrentOrRpsExceed = !flowStat.incrRequest(service, currentTimeSlot, config.concurrents, config.qps);
                if (concurrentOrRpsExceed ) {
                    StringBuilder b = ThreadContext.getStringBuilder();
                    b.append(service).append(Constants.Symbol.SPACE).append(WebUtils.getClientReqPath(exchange));
                    b.append(exceed)                             .append(config.resource)          .append(concurrents).append(config.concurrents).append(orQps).append(config.qps);
                    log.warn(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId());

                    ResourceRateLimitConfig gc = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.GLOBAL);

                    ServerHttpResponse resp = exchange.getResponse();
                    resp.setStatusCode(HttpStatus.OK);
                    resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, gc.responseType);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(gc.responseContent.getBytes())));
                }
            }

            // 没配置或没超配置
            long start = exchange.getAttribute("start");
            return chain.filter(exchange)
                    .doOnSuccess(
                            r -> {
                                inTheEnd(exchange, start, currentTimeSlot, true);
                            }
                    )
                    .doOnError(
                            t -> {
                                inTheEnd(exchange, start, currentTimeSlot, false);
                            }
                    )
                    .doOnCancel(
                            () -> {
                                inTheEnd(exchange, start, currentTimeSlot, false);
                            }
                    );
        }

        return chain.filter(exchange);
    }

    private void inTheEnd(ServerWebExchange exchange, long start, long currentTimeSlot, boolean success) {
        long spend = System.currentTimeMillis() - start;
        String service = WebUtils.getClientService(exchange);
        flowStat.decrConcurrentRequest(service, currentTimeSlot);
        flowStat.addRequestRT(service, currentTimeSlot, spend, success);
    }
}
