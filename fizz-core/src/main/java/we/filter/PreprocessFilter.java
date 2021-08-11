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

import com.google.common.collect.BoundType;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.FixedPluginFilter;
import we.plugin.FizzPluginFilterChain;
import we.plugin.PluginFilter;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.AuthPluginFilter;
import we.plugin.stat.StatPluginFilter;
import we.util.ConvertedRequestBodyDataBufferWrapper;
import we.util.NettyDataBufferUtils;
import we.util.ReactorUtils;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author hongqiaowei
 */

@Component(PreprocessFilter.PREPROCESS_FILTER)
@Order(10)
public class PreprocessFilter extends FizzWebFilter {

    private static final Logger log = LoggerFactory.getLogger(PreprocessFilter.class);

    public  static final String       PREPROCESS_FILTER = "preprocessFilter";

    private static final FilterResult succFr            = FilterResult.SUCCESS(PREPROCESS_FILTER);

    @Resource(name = StatPluginFilter.STAT_PLUGIN_FILTER)
    private StatPluginFilter statPluginFilter;

    @Resource(name = AuthPluginFilter.AUTH_PLUGIN_FILTER)
    private AuthPluginFilter authPluginFilter;

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        Map<String, FilterResult> fc         = new HashMap<>();                  fc.put(WebUtils.PREV_FILTER_RESULT, succFr);
        Map<String, String>       appendHdrs = new HashMap<>(8);
        Map<String, Object>       eas        = exchange.getAttributes();        eas.put(WebUtils.FILTER_CONTEXT,     fc);
                                                                                eas.put(WebUtils.APPEND_HEADERS,     appendHdrs);

        ServerHttpRequest req = exchange.getRequest();
        return NettyDataBufferUtils.join(req.getBody()).defaultIfEmpty(WebUtils.EMPTY_BODY)
                .flatMap(
                        body -> {
                            if (body != WebUtils.EMPTY_BODY && body.readableByteCount() > 0) {
                                try {
                                    byte[] bytes = new byte[body.readableByteCount()];
                                    body.read(bytes);
                                    DataBuffer retain = NettyDataBufferUtils.from(bytes);
                                    eas.put(WebUtils.REQUEST_BODY, retain);
                                } finally {
                                    NettyDataBufferUtils.release(body);
                                }
                            }
                            Mono vm = statPluginFilter.filter(exchange, null, null);
                            return process(exchange, chain, eas, vm);
                        }
                )
                .doFinally(
                        s -> {
                            Object convertedRequestBody = WebUtils.getConvertedRequestBody(exchange);
                            if (convertedRequestBody instanceof ConvertedRequestBodyDataBufferWrapper) {
                                DataBuffer b = ((ConvertedRequestBodyDataBufferWrapper) convertedRequestBody).body;
                                if (b != null) {
                                    boolean release = NettyDataBufferUtils.release(req.getId(), b);
                                    if (log.isDebugEnabled()) {
                                        log.debug("release converted request body databuffer " + release, LogService.BIZ_ID, req.getId());
                                    }
                                }
                            }
                        }
                );
    }

    // TODO
    private Mono<Void> process(ServerWebExchange exchange, WebFilterChain chain, Map<String, Object> eas, Mono vm) {
        return chain(exchange, vm, authPluginFilter).defaultIfEmpty(ReactorUtils.NULL)
                .flatMap(
                        v -> {
                            Object authRes = WebUtils.getFilterResultDataItem(exchange, AuthPluginFilter.AUTH_PLUGIN_FILTER, AuthPluginFilter.RESULT);
                            Mono m = ReactorUtils.getInitiateMono();
                            if (authRes instanceof ApiConfig) {
                                ApiConfig ac = (ApiConfig) authRes;
                                afterAuth(exchange, ac);
                                m = executeFixedPluginFilters(exchange);
                                m = m.defaultIfEmpty(ReactorUtils.NULL);
                                if (ac.pluginConfigs == null || ac.pluginConfigs.isEmpty()) {
                                    return m.flatMap(func(exchange, chain));
                                } else {
                                    return m.flatMap(
                                                nil -> {
                                                    eas.put(FizzPluginFilterChain.WEB_FILTER_CHAIN, chain);
                                                    return FizzPluginFilterChain.next(exchange);
                                                }
                                    );
                                }
                            } else if (authRes == ApiConfigService.Access.YES) {
                                afterAuth(exchange, null);
                                m = executeFixedPluginFilters(exchange);
                                return m.defaultIfEmpty(ReactorUtils.NULL).flatMap(func(exchange, chain));
                            } else {
                                String err = null;
                                if (authRes instanceof ApiConfigService.Access) {
                                    ApiConfigService.Access access = (ApiConfigService.Access) authRes;
                                    err = access.getReason();
                                } else {
                                    err = authRes.toString();
                                }
                                return WebUtils.responseError(exchange, HttpStatus.FORBIDDEN.value(), err);
                            }
                        }
                );
    }

    private void afterAuth(ServerWebExchange exchange, ApiConfig ac) {
        String bs = null, bp = null;
        if (ac == null) {
            bs = WebUtils.getClientService(exchange);
            bp = WebUtils.getClientReqPath(exchange);
        } else {
            if (ac.type != ApiConfig.Type.CALLBACK) {
                if (ac.type != ApiConfig.Type.REVERSE_PROXY) {
                    bs = ac.backendService;
                }
                if (ac.type != ApiConfig.Type.DUBBO) {
                    bp = ac.transform(WebUtils.getClientReqPath(exchange));
                }
            }
        }
        if (bs != null) {
            WebUtils.setBackendService(exchange, bs);
        }
        if (bp != null) {
            WebUtils.setBackendPath(exchange, bp);
        }
    }

    private Mono chain(ServerWebExchange exchange, Mono m, PluginFilter pf) {
        return m.defaultIfEmpty(ReactorUtils.NULL).flatMap(
                v -> {
                    return pf.filter(exchange, null, null);
                }
        );
    }

    private Function func(ServerWebExchange exchange, WebFilterChain chain) {
        return v -> {
            Mono<Void> dr = WebUtils.getDirectResponse(exchange);
            if (dr != null) {
                return dr;
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> executeFixedPluginFilters(ServerWebExchange exchange) {
        Mono vm = Mono.empty();
        List<FixedPluginFilter> fixedPluginFilters = FixedPluginFilter.getPluginFilters();
        for (byte i = 0; i < fixedPluginFilters.size(); i++) {
            FixedPluginFilter fpf = fixedPluginFilters.get(i);
            vm = vm.defaultIfEmpty(ReactorUtils.NULL).flatMap(
                    v -> {
                        return fpf.filter(exchange, null, null);
                    }
            );
        }
        return vm;
    }
}
