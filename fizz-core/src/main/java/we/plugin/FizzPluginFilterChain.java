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

package we.plugin;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.FizzAppContext;
import we.util.ReactorUtils;
import we.util.WebUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public final class FizzPluginFilterChain {

    private static final String pluginFilterConfigsIt = "pfcsit";

    public  static final String WEB_FILTER_CHAIN      = "wfc";

    private FizzPluginFilterChain() {
    }

    public static Mono<Void> next(ServerWebExchange exchange) {
        Map<String, Object> attris = exchange.getAttributes();
        List<PluginConfig> pfcs = WebUtils.getApiConfig(exchange).pluginConfigs;
        Iterator<PluginConfig> it = (Iterator<PluginConfig>) attris.get(pluginFilterConfigsIt);
        if (it == null) {
            it = pfcs.iterator();
            attris.put(pluginFilterConfigsIt, it);
        }
        Mono r;
        if (it.hasNext()) {
            PluginConfig pfc = it.next();
            FizzPluginFilter pf = FizzAppContext.appContext.getBean(pfc.plugin, FizzPluginFilter.class);
            r = pf.filter(exchange, pfc.config);
        } else {
            // attris.remove(pluginFilterConfigsIt);
            WebFilterChain chain = (WebFilterChain) attris.get(WEB_FILTER_CHAIN);
            r = chain.filter(exchange);
        }
        return r.defaultIfEmpty(ReactorUtils.NULL);
    }
}
