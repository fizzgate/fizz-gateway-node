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

package com.fizzgate.plugin.stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fizzgate.plugin.PluginFilter;
import com.fizzgate.util.Consts;
import com.fizzgate.util.DateTimeUtils;
import com.fizzgate.util.ThreadContext;
import com.fizzgate.util.WebUtils;

import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Component(StatPluginFilter.STAT_PLUGIN_FILTER)
public class StatPluginFilter extends PluginFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatPluginFilter.class);

    public static final String STAT_PLUGIN_FILTER = "statPlugin";

    @Resource
    private StatPluginFilterProperties statPluginFilterProperties;

    private Map<Long/*thread id*/,
                                      Map/*LinkedHashMap*/<Long/*time win start*/,
                                                                                      Map<String/*service+apiMethod+apiPath*/, AccessStat>
                                      >
            >
            threadTimeWinAccessStatMap = new HashMap<>();

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {

        if (statPluginFilterProperties.isStatOpen()) {
            long tid = Thread.currentThread().getId();
            Map<Long, Map<String, AccessStat>> timeWinAccessStatMap = threadTimeWinAccessStatMap.get(tid);
            if (timeWinAccessStatMap == null) {
                timeWinAccessStatMap = new LinkedHashMap<Long, Map<String, AccessStat>>(4, 1) {
                                           @Override
                                           protected boolean removeEldestEntry(Map.Entry eldest) {
                                                return size() > 2;
                                            }
                                       };
                threadTimeWinAccessStatMap.put(tid, timeWinAccessStatMap);
            }

            long currentTimeWinStart = DateTimeUtils.get10sTimeWinStart(1);
            Map<String, AccessStat> accessStatMap = timeWinAccessStatMap.computeIfAbsent(currentTimeWinStart, k -> new HashMap<>(128));

            String service = WebUtils.getClientService(exchange);
            String method  = exchange.getRequest().getMethodValue();
            String path    = WebUtils.getClientReqPath(exchange);
            String key     = ThreadContext.getStringBuilder().append(service).append(method).append(path).toString();
            AccessStat accessStat = accessStatMap.get(key);
            if (accessStat == null) {
                accessStat = new AccessStat();
                accessStat.service   = service;
                accessStat.apiMethod = method;
                accessStat.apiPath   = path;
                accessStatMap.put(key, accessStat);
            }
            accessStat.reqTime = System.currentTimeMillis();
            accessStat.reqs++;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("update access stat: {}, which request at {}", accessStat, DateTimeUtils.convert(accessStat.reqTime, Consts.DP.DP19));
            }
        }

        return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, STAT_PLUGIN_FILTER, null);
    }

    public Map<String, AccessStat> getAccessStat(long timeWinStart) {
        Map<String, AccessStat> result = ThreadContext.getHashMap();
        threadTimeWinAccessStatMap.forEach(
                (t, timeWinAccessStatMap) -> {
                    Map<String, AccessStat> accessStatMap = timeWinAccessStatMap.get(timeWinStart);
                    if (accessStatMap != null) {
                        accessStatMap.forEach(
                                (smp, accessStat) -> {
                                    AccessStat as = result.get(smp);
                                    if (as == null) {
                                        accessStat.start = timeWinStart;
                                        result.put(smp, accessStat);
                                    } else {
                                        as.reqs = as.reqs + accessStat.reqs;
                                        if (accessStat.reqTime > as.reqTime) {
                                            as.reqTime = accessStat.reqTime;
                                        }
                                    }
                                }
                        );
                    }
                }
        );
        return result;
    }
}
