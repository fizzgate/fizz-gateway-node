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

package we.config;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;

import org.springframework.util.ObjectUtils;
import we.plugin.auth.GatewayGroup;
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.NetworkUtils;
import we.util.WebUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.management.RuntimeErrorException;
import java.util.*;

/**
 * @author hongqiaowei
 */

@Configuration
public class SystemConfig {

    private static final Logger log = LoggerFactory.getLogger(SystemConfig.class);

    @Value("${log.response-body:false}")
    private boolean                     logResponseBody;

    @Value("${log.headers:x}")
    private String                      logHeaders;

    private Set<String>                 logHeaderSet                  = new HashSet<>();

    // @Value("${gateway-group:}")
    // private String                      gatewayGroup;
    //
    // private Map<String, Set<Character>> server2gatewayGroupSetMap     = new HashMap<>();
    //
    // private Set<Character>              currentServerGatewayGroupSet;

    @NacosValue(value = "${spring.profiles.active}")
    @Value("${spring.profiles.active}")
    private String profile;

    // public Set<Character> getCurrentServerGatewayGroupSet() {
    //     return currentServerGatewayGroupSet;
    // }

    public Set<String> getLogHeaderSet() {
        return logHeaderSet;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        afterLogResponseBodySet();
        afterLogHeadersSet();
        // afterGatewayGroupSet();
    }

    private void afterLogResponseBodySet() {
        WebUtils.logResponseBody = logResponseBody;
        log.info("log response body: " + logResponseBody);
    }

    private void afterLogHeadersSet() {
        logHeaderSet.clear();
        Arrays.stream(StringUtils.split(logHeaders, Constants.Symbol.COMMA)).forEach(h -> {
            logHeaderSet.add(h);
        });
        WebUtils.logHeaderSet = logHeaderSet;
        log.info("log header list: " + logHeaderSet.toString());
    }

    // private void afterGatewayGroupSet() {
    //     server2gatewayGroupSetMap.clear();
    //     if (StringUtils.isNotBlank(gatewayGroup)) {
    //         Arrays.stream(StringUtils.split(gatewayGroup, ';')).forEach(
    //                 gg -> {
    //                     Character group = Character.valueOf(gg.charAt(0));
    //                     String servers = gg.substring(gg.indexOf(':') + 1);
    //                     Arrays.stream(StringUtils.split(servers, ',')).forEach(
    //                             s -> {
    //                                 Set<Character> gs = server2gatewayGroupSetMap.get(s);
    //                                 if (gs == null) {
    //                                     gs = new HashSet<>();
    //                                     server2gatewayGroupSetMap.put(s, gs);
    //                                 }
    //                                 gs.add(group);
    //                             }
    //                     );
    //                 }
    //         );
    //     }
    //     log.info("server 2 gateway group set map: " + JacksonUtils.writeValueAsString(server2gatewayGroupSetMap));
    //     String serverIp = NetworkUtils.getServerIp();
    //     currentServerGatewayGroupSet = server2gatewayGroupSetMap.get(serverIp);
    //     if (currentServerGatewayGroupSet == null) {
    //         if (Constants.Profiles.DEV.equals(profile) || Constants.Profiles.TEST.equals(profile)) {
    //             currentServerGatewayGroupSet = new HashSet<>();
    //             currentServerGatewayGroupSet.add(GatewayGroup.C);
    //             currentServerGatewayGroupSet.add(GatewayGroup.B);
    //             currentServerGatewayGroupSet.add(GatewayGroup.T);
    //             server2gatewayGroupSetMap.put(serverIp, currentServerGatewayGroupSet);
    //         } else {
    //             throw new RuntimeException("no gateway group config for " + serverIp);
    //         }
    //     }
    //     log.info("current server: " + serverIp + ", belong to: " + currentServerGatewayGroupSet);
    // }

    @ApolloConfigChangeListener
    private void configChangeListter(ConfigChangeEvent cce) {
        cce.changedKeys().forEach(
                k -> {
                    ConfigChange c = cce.getChange(k);
                    String p = c.getPropertyName();
                    String ov = c.getOldValue();
                    String nv = c.getNewValue();
                    log.info(p + " old: " + ov + ", new: " + nv);
                    if (p.equals("log.response-body")) {
                        this.updateLogResponseBody(Boolean.parseBoolean(nv));
                    } else if (p.equals("log.headers")) {
                        this.updateLogHeaders(nv);
                    } /*else if (p.equals("gateway-group")) {
                        gatewayGroup = nv;
                        afterGatewayGroupSet();
                    }*/
                }
        );
    }

    private void updateLogResponseBody(boolean newValue) {
        logResponseBody = newValue;
        this.afterLogResponseBodySet();
    }

    private void updateLogHeaders(String newValue) {
        logHeaders = newValue;
        afterLogHeadersSet();
    }

    @NacosValue(value = "${log.response-body:false}", autoRefreshed = true)
    public void setLogResponseBody(boolean logResponseBody) {
        if (this.logResponseBody == logResponseBody) {
            return;
        }
        log.info("log.response-body old: " + this.logResponseBody + ", new: " + logResponseBody);
        this.updateLogResponseBody(logResponseBody);
    }

    @NacosValue(value = "${log.headers:x}", autoRefreshed = true)
    public void setLogHeaders(String logHeaders) {
        if (ObjectUtils.nullSafeEquals(this.logHeaders, logHeaders)) {
            return;
        }
        log.info("log.headers old: " + this.logHeaders + ", new: " + logHeaders);
        this.updateLogHeaders(logHeaders);
    }
}
