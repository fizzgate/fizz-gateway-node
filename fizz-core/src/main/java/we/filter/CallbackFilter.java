/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.filter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.CallbackConfig;
import we.plugin.auth.GatewayGroupService;
import we.plugin.auth.Receiver;
import we.proxy.CallbackService;
import we.proxy.DiscoveryClientUriSelector;
import we.proxy.ServiceInstance;
import we.util.Constants;
import we.util.NettyDataBufferUtils;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

/**
 * @author hongqiaowei
 */

@Component(CallbackFilter.CALLBACK_FILTER)
@Order(20)
public class CallbackFilter extends FizzWebFilter {

    private static final Logger     log             = LoggerFactory.getLogger(CallbackFilter.class);

    public  static final String     CALLBACK_FILTER = "callbackFilter";

    private static final String     s2im            = "$s2im";

    private static final String     json            = "json";

    @Resource
    private DiscoveryClientUriSelector discoveryClientSelector;

    @Resource
    private CallbackFilterProperties callbackFilterProperties;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private CallbackService callbackService;

    @Resource
    private GatewayGroupService gatewayGroupService;

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        ApiConfig ac = WebUtils.getApiConfig(exchange);
        if (ac != null && ac.type == ApiConfig.Type.CALLBACK) {
            CallbackConfig cc = ac.callbackConfig;
            ServerHttpRequest req = exchange.getRequest();
            return
                    DataBufferUtils.join(req.getBody()).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                            .flatMap(
                                    b -> {
                                        DataBuffer body = null;
                                        if (b != NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                            if (b instanceof PooledDataBuffer) {
                                                byte[] bytes = new byte[b.readableByteCount()];
                                                try {
                                                    b.read(bytes);
                                                    body = NettyDataBufferUtils.from(bytes);
                                                } finally {
                                                    NettyDataBufferUtils.release(b);
                                                }
                                            } else {
                                                body = b;
                                            }
                                        }
                                        HashMap<String, ServiceInstance> service2instMap = getService2instMap(ac);
                                        HttpHeaders headers = WebUtils.mergeAppendHeaders(exchange);
                                        pushReq2manager(exchange, headers, body, service2instMap, cc.id, ac.gatewayGroups.iterator().next());
                                        if (cc.type == CallbackConfig.Type.ASYNC || StringUtils.isNotBlank(cc.respBody)) {
                                            return directResponse(exchange, cc);
                                        } else {
                                            return callbackService.requestBackends(exchange, headers, body, cc, service2instMap);
                                        }
                                    }
                            )
                    ;
        }
        return chain.filter(exchange);
    }

    private Mono<Void> directResponse(ServerWebExchange exchange, CallbackConfig cc) {
        HttpHeaders httpHeaders = new HttpHeaders();
        cc.respHeaders.forEach(
                (h, v) -> {
                    httpHeaders.addAll(h, v);
                }
        );
        return WebUtils.buildDirectResponse(exchange.getResponse(), HttpStatus.OK, httpHeaders, cc.respBody);
    }

    private HashMap<String, ServiceInstance> getService2instMap(ApiConfig ac) {
        HashMap<String, ServiceInstance> service2instMap = ThreadContext.getHashMap(s2im, String.class, ServiceInstance.class);
        List<Receiver> receivers = ac.callbackConfig.receivers;
        for (Receiver r : receivers) {
            if (r.type == ApiConfig.Type.SERVICE_DISCOVERY) {
                ServiceInstance inst = discoveryClientSelector.getNextInstance(r.service);
                service2instMap.put(r.service, inst);
            }
        }
        return service2instMap;
    }

    private static final String _id                = "\"id\":";
    private static final String _datetime          = "\"datetime\":";
    private static final String _origin            = "\"origin\":";
    private static final String _app               = "\"app\":";
    private static final String _method            = "\"method\":";
    private static final String _service           = "\"service\":";
    private static final String _path              = "\"path\":";
    private static final String _query             = "\"query\":";
    private static final String _headers           = "\"headers\":";
    private static final String _body              = "\"body\":";
    private static final String _callbackConfigId  = "\"callbackConfigId\":";
    private static final String _receivers         = "\"receivers\":";
    private static final String _gatewayGroup      = "\"gatewayGroup\":";

    private void pushReq2manager(ServerWebExchange exchange, HttpHeaders headers, DataBuffer body, HashMap<String, ServiceInstance> service2instMap, int callbackConfigId,
                                 String gatewayGroup) {

        ServerHttpRequest req = exchange.getRequest();
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(Constants.Symbol.LEFT_BRACE);

        b.append(_id);                     toJsonStringValue(b, req.getId());                                                      b.append(Constants.Symbol.COMMA);
        b.append(_datetime);               b.append(System.currentTimeMillis());                                                   b.append(Constants.Symbol.COMMA);
        b.append(_origin);                 toJsonStringValue(b, WebUtils.getOriginIp(exchange));                                   b.append(Constants.Symbol.COMMA);

        String appId = WebUtils.getAppId(exchange);
        if (appId != null) {
        b.append(_app);                    toJsonStringValue(b, appId);                                                            b.append(Constants.Symbol.COMMA);
        }

        b.append(_method);                 toJsonStringValue(b, req.getMethod().name());                                           b.append(Constants.Symbol.COMMA);
        b.append(_service);                toJsonStringValue(b, WebUtils.getClientService(exchange));                              b.append(Constants.Symbol.COMMA);
        b.append(_path);                   toJsonStringValue(b, WebUtils.getClientReqPath(exchange));                              b.append(Constants.Symbol.COMMA);

        String query = WebUtils.getClientReqQuery(exchange);
        if (query != null) {
        b.append(_query);                  toJsonStringValue(b, query);                                                            b.append(Constants.Symbol.COMMA);
        }

        String headersJson = JSON.toJSONString(headers);
        b.append(_headers);                b.append(headersJson);                                                                  b.append(Constants.Symbol.COMMA);

        b.append(_callbackConfigId);       b.append(callbackConfigId);                                                             b.append(Constants.Symbol.COMMA);

        if (!service2instMap.isEmpty()) {
        String rs = JSON.toJSONString(JSON.toJSONString(service2instMap));
        b.append(_receivers);              b.append(rs);                                                                           b.append(Constants.Symbol.COMMA);
        }

        // String gg = gatewayGroupService.currentGatewayGroupSet.iterator().next();
        b.append(_gatewayGroup);           toJsonStringValue(b, gatewayGroup);

        if (body != null) {
                                                                                                                                   b.append(Constants.Symbol.COMMA);
        String bodyStr = body.toString(StandardCharsets.UTF_8);
        MediaType contentType = req.getHeaders().getContentType();
        if (contentType != null && contentType.getSubtype().equalsIgnoreCase(json)) {
            b.append(_body);                   b.append(JSON.toJSONString(bodyStr));
        } else {
            b.append(_body);                   toJsonStringValue(b, bodyStr);
        }
        }

        b.append(Constants.Symbol.RIGHT_BRACE);
        String msg = b.toString();
        if ("kafka".equals(callbackFilterProperties.getDest())) { // for internal use
            log.warn(msg, LogService.HANDLE_STGY, LogService.toKF(callbackFilterProperties.getQueue()));
        } else {
            rt.convertAndSend(callbackFilterProperties.getQueue(), msg).subscribe();
        }
        if (log.isDebugEnabled()) {
            log.debug("push callback req: " + msg);
        }
    }

    private static void toJsonStringValue(StringBuilder b, String value) {
        b.append(Constants.Symbol.DOUBLE_QUOTE).append(value).append(Constants.Symbol.DOUBLE_QUOTE);
    }
}
