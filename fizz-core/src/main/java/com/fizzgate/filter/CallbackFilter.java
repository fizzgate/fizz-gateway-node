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

package com.fizzgate.filter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import com.alibaba.fastjson.JSON;
import com.fizzgate.config.AggregateRedisConfig;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.auth.CallbackConfig;
import com.fizzgate.plugin.auth.GatewayGroupService;
import com.fizzgate.plugin.auth.Receiver;
import com.fizzgate.proxy.CallbackService;
import com.fizzgate.proxy.DiscoveryClientUriSelector;
import com.fizzgate.proxy.ServiceInstance;
import com.fizzgate.service_registry.RegistryCenterService;
import com.fizzgate.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import com.fizzgate.spring.web.server.ext.FizzServerWebExchangeDecorator;
import com.fizzgate.util.Consts;
import com.fizzgate.util.NettyDataBufferUtils;
import com.fizzgate.util.ThreadContext;
import com.fizzgate.util.WebUtils;
import reactor.core.publisher.Mono;

/**
 * @author hongqiaowei
 */

@Component(CallbackFilter.CALLBACK_FILTER)
@Order(20)
public class CallbackFilter extends FizzWebFilter {

    private static final Logger     log             = LoggerFactory.getLogger(CallbackFilter.class);
    private static final Logger     CALLBACK_LOGGER = LoggerFactory.getLogger("callback");

    public  static final String     CALLBACK_FILTER = "callbackFilter";

    private static final String     s2im            = "s2imT";

    private static final String     json            = "json";

    @Resource
    private DiscoveryClientUriSelector discoveryClientSelector;

    @Resource
    private CallbackFilterProperties callbackFilterProperties;

    @Resource
    private RegistryCenterService registryCenterService;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private CallbackService callbackService;

    @Resource
    private GatewayGroupService gatewayGroupService;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        if (WebUtils.isAdminReq(exchange) || WebUtils.isFizzReq(exchange) || WebUtils.isFavReq(exchange)) {
            return chain.filter(exchange);
        }

    	String traceId = WebUtils.getTraceId(exchange);
    	org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);

        ServerHttpRequest req = exchange.getRequest();
        if (req instanceof FizzServerHttpRequestDecorator) {
            return doFilter(exchange, chain);
        }
        return
                NettyDataBufferUtils.join(req.getBody()).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                        .flatMap(
                                body -> {
                                    FizzServerHttpRequestDecorator requestDecorator = new FizzServerHttpRequestDecorator(req);
                                    if (body != NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                        try {
                                            requestDecorator.setBody(body);
                                        } finally {
                                            NettyDataBufferUtils.release(body);
                                        }
                                    }
                                    ServerWebExchange mutatedExchange = exchange.mutate().request(requestDecorator).build();
                                    ServerWebExchange newExchange = mutatedExchange;
                                    MediaType contentType = req.getHeaders().getContentType();
                                    if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
                                        newExchange = new FizzServerWebExchangeDecorator(mutatedExchange);
                                    }
                                    return doFilter(newExchange, chain);
                                }
                        );
    }
    
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
    	String traceId = WebUtils.getTraceId(exchange);
    	org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
        
        ApiConfig ac = WebUtils.getApiConfig(exchange);
        if (ac != null && ac.type == ApiConfig.Type.CALLBACK) {
            CallbackConfig cc = ac.callbackConfig;
            FizzServerHttpRequestDecorator req = (FizzServerHttpRequestDecorator) exchange.getRequest();
            return req.getBody().defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER).single().flatMap(b -> {
            							String body = b.toString(StandardCharsets.UTF_8);
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
        return WebUtils.response(exchange.getResponse(), HttpStatus.OK, httpHeaders, cc.respBody);
    }

    private HashMap<String, ServiceInstance> getService2instMap(ApiConfig ac) {
        HashMap<String, ServiceInstance> service2instMap = ThreadContext.getHashMap(s2im);
        List<Receiver> receivers = ac.callbackConfig.receivers;
        for (Receiver r : receivers) {
            if (r.type == ApiConfig.Type.SERVICE_DISCOVERY) {
                ServiceInstance inst = null;
                if (r.registryCenter == null) {
                    inst = discoveryClientSelector.getNextInstance(r.service);
                } else {
                    String instance = registryCenterService.getInstance(r.registryCenter, r.service);
                    String[] ipAndPort = StringUtils.split(instance, Consts.S.COLON);
                    inst = new ServiceInstance(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                }
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

    private void pushReq2manager(ServerWebExchange exchange, HttpHeaders headers, Object body, HashMap<String, ServiceInstance> service2instMap, int callbackConfigId,
                                 String gatewayGroup) {

        ServerHttpRequest req = exchange.getRequest();
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(Consts.S.LEFT_BRACE);

        b.append(_id);                     toJsonStringValue(b, WebUtils.getTraceId(exchange));                                    b.append(Consts.S.COMMA);
        b.append(_datetime);               b.append(System.currentTimeMillis());                                                   b.append(Consts.S.COMMA);
        b.append(_origin);                 toJsonStringValue(b, WebUtils.getOriginIp(exchange));                                   b.append(Consts.S.COMMA);

        String appId = WebUtils.getAppId(exchange);
        if (appId != null) {
        b.append(_app);                    toJsonStringValue(b, appId);                                                            b.append(Consts.S.COMMA);
        }

        b.append(_method);                 toJsonStringValue(b, req.getMethod().name());                                           b.append(Consts.S.COMMA);
        b.append(_service);                toJsonStringValue(b, WebUtils.getClientService(exchange));                              b.append(Consts.S.COMMA);
        b.append(_path);                   toJsonStringValue(b, WebUtils.getClientReqPath(exchange));                              b.append(Consts.S.COMMA);

        String query = WebUtils.getClientReqQuery(exchange);
        if (query != null) {
        b.append(_query);                  toJsonStringValue(b, query);                                                            b.append(Consts.S.COMMA);
        }

        String headersJson = JSON.toJSONString(headers);
        b.append(_headers);                b.append(headersJson);                                                                  b.append(Consts.S.COMMA);

        b.append(_callbackConfigId);       b.append(callbackConfigId);                                                             b.append(Consts.S.COMMA);

        if (!service2instMap.isEmpty()) {
        String rs = JSON.toJSONString(JSON.toJSONString(service2instMap));
        b.append(_receivers);              b.append(rs);                                                                           b.append(Consts.S.COMMA);
        }

        // String gg = gatewayGroupService.currentGatewayGroupSet.iterator().next();
        b.append(_gatewayGroup);           toJsonStringValue(b, gatewayGroup);

        if (body != null) {
                                                                                                                                   b.append(Consts.S.COMMA);
//        String bodyStr = body.toString(StandardCharsets.UTF_8);
        String bodyStr = body.toString();
        MediaType contentType = req.getHeaders().getContentType();
        if (contentType != null && contentType.getSubtype().equalsIgnoreCase(json)) {
        b.append(_body);                   b.append(JSON.toJSONString(bodyStr));
        } else {
        b.append(_body);                   toJsonStringValue(b, bodyStr);
        }
        }

        b.append(Consts.S.RIGHT_BRACE);
        String msg = b.toString();
        if ("kafka".equals(callbackFilterProperties.getDest())) { // for internal use
            // log.warn(msg, LogService.HANDLE_STGY, LogService.toKF(callbackFilterProperties.getQueue()));
            CALLBACK_LOGGER.info(msg);
        } else {
            rt.convertAndSend(callbackFilterProperties.getQueue(), msg).subscribe();
        }
        if (log.isDebugEnabled()) {
            log.debug("push callback req: " + msg);
        }
    }

    private static void toJsonStringValue(StringBuilder b, String value) {
        b.append(Consts.S.DOUBLE_QUOTE).append(value).append(Consts.S.DOUBLE_QUOTE);
    }
}
