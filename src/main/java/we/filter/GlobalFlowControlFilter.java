package we.filter;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

@Component(GlobalFlowControlFilter.GLOBAL_FLOW_CONTROL_FILTER)
@Order(-4)
public class GlobalFlowControlFilter extends ProxyAggrFilter {

    private static final Logger log                 = LoggerFactory.getLogger(GlobalFlowControlFilter.class);

    public  static final String GLOBAL_FLOW_CONTROL_FILTER = "globalFlowControlFilter";

        private static final String exceed              = " exceed ";
    private static final String concurrents         = " concurrents ";
    private static final String orQps               = " or qps ";

        @NacosValue(value = "${flowControl:false}", autoRefreshed = true)
    @Value("${flowControl:false}")
    private boolean flowControl;

        @Resource
    private ResourceRateLimitConfigService resourceRateLimitConfigService;

    public static FlowStat flowStat = new FlowStat();


    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        if (flowControl) {
            long currentTimeSlot = flowStat.currentTimeSlotId();
            exchange.getAttributes().put("currentTimeSlot", currentTimeSlot);
            ResourceRateLimitConfig config = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.GLOBAL);
            if (config.isEnable()) {
                // 有流控配置


                boolean concurrentOrRpsExceed = !flowStat.incrRequest(ResourceRateLimitConfig.GLOBAL, currentTimeSlot, config.concurrents, config.qps);
                if (concurrentOrRpsExceed) {
                    // 如果超了，直接响应

                                    StringBuilder b = ThreadContext.getStringBuilder();
                b.append(WebUtils.getClientService(exchange)).append(Constants.Symbol.SPACE).append(WebUtils.getClientReqPath(exchange));
                b.append(exceed)                             .append(config.resource)          .append(concurrents).append(config.concurrents).append(orQps).append(config.qps);
                log.warn(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId());

                ServerHttpResponse resp = exchange.getResponse();
                resp.setStatusCode(HttpStatus.OK);
                resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, config.responseType);
                return resp.writeWith(Mono.just(resp.bufferFactory().wrap(config.responseContent.getBytes())));
                }

            } else {
                flowStat.incrRequest(ResourceRateLimitConfig.GLOBAL, currentTimeSlot, null, null);
            }

            // 没配置或没超配置
                            long start = System.currentTimeMillis();
            exchange.getAttributes().put("start", start);
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
            flowStat.decrConcurrentRequest(ResourceRateLimitConfig.GLOBAL, currentTimeSlot);
            flowStat.addRequestRT(ResourceRateLimitConfig.GLOBAL, currentTimeSlot, spend, success);
    }
}
