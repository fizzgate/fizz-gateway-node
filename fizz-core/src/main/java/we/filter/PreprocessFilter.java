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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.plugin.FixedPluginFilter;
import we.plugin.FizzPluginFilterChain;
import we.plugin.PluginFilter;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.AuthPluginFilter;
import we.plugin.stat.StatPluginFilter;
import we.proxy.Route;
import we.util.ReactorUtils;
import we.util.Result;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.util.Collections;
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

        Mono vm = statPluginFilter.filter(exchange, null, null);
        return process(exchange, chain, eas, vm);
    }

    private Mono<Void> process(ServerWebExchange exchange, WebFilterChain chain, Map<String, Object> eas, Mono vm) {
        return chain(exchange, vm, authPluginFilter).defaultIfEmpty(ReactorUtils.NULL)
               .flatMap(
                       v -> {
                           Result<ApiConfig> authRes = (Result<ApiConfig>) WebUtils.getFilterResultDataItem(exchange, AuthPluginFilter.AUTH_PLUGIN_FILTER, AuthPluginFilter.RESULT);
                           if (authRes.code == Result.FAIL) {
                               return WebUtils.responseError(exchange, HttpStatus.FORBIDDEN.value(), authRes.msg);
                           }
                           Mono m = ReactorUtils.getInitiateMono();
                           ApiConfig ac = authRes.data;
                           if (ac == null) {
                               afterAuth(exchange, null, null);
                               m = executeFixedPluginFilters(exchange);
                               return m.defaultIfEmpty(ReactorUtils.NULL).flatMap(func(exchange, chain));
                           }
                           Route route = ac.getRoute(exchange);
                           eas.put(WebUtils.ROUTE, route);
                           afterAuth(exchange, ac, route);
                           m = executeFixedPluginFilters(exchange);
                           m = m.defaultIfEmpty(ReactorUtils.NULL);
                           if (CollectionUtils.isEmpty(route.pluginConfigs)) {
                               return m.flatMap(func(exchange, chain));
                           } else {
                               return m.flatMap(
                                          nil -> {
                                              eas.put(FizzPluginFilterChain.WEB_FILTER_CHAIN, chain);
                                              return FizzPluginFilterChain.next(exchange);
                                          }
                                      );
                           }
                       }
               );
    }

    private void afterAuth(ServerWebExchange exchange, ApiConfig ac, Route route) {
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
                    route.backendPath = bp;
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
