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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import we.util.Constants;
import we.util.WebUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

@Configuration
public class SystemConfig {

    private static final Logger log = LoggerFactory.getLogger(SystemConfig.class);

    public  static final String DEFAULT_GATEWAY_PREFIX      = "/proxy";

    public  static final String DEFAULT_GATEWAY_TEST_PREFIX = "/_proxytest";

    public  String       gatewayPrefix    = DEFAULT_GATEWAY_PREFIX;

    public  List<String> appHeaders       = Stream.of("fizz-appid").collect(Collectors.toList());

    public  List<String> signHeaders      = Stream.of("fizz-sign") .collect(Collectors.toList());

    public  List<String> timestampHeaders = Stream.of("fizz-ts")   .collect(Collectors.toList());
    
    public  List<String> proxySetHeaders  = new ArrayList<>();
    
    
    @NacosValue(value = "${gateway.aggr.proxy_set_headers:}", autoRefreshed = true)
    @Value("${gateway.aggr.proxy_set_headers:}")
    public void setProxySetHeaders(String hdrs) {
        if (StringUtils.isNotBlank(hdrs)) {
            proxySetHeaders.clear();
            for (String h : StringUtils.split(hdrs, ',')) {
            	proxySetHeaders.add(h.trim());
            }
        }
        log.info("proxy set headers: " + hdrs);
    }

    @NacosValue(value = "${gateway.prefix:/proxy}", autoRefreshed = true)
    @Value(             "${gateway.prefix:/proxy}"                      )
    public void setGatewayPrefix(String gp) {
        gatewayPrefix = gp;
        WebUtils.setGatewayPrefix(gatewayPrefix);
        log.info("gateway prefix: " + gatewayPrefix);
    }

    @NacosValue(value = "${custom.header.appid:}", autoRefreshed = true)
    @Value(             "${custom.header.appid:}"                      )
    public void setCustomAppHeaders(String hdrs) {
        if (StringUtils.isNotBlank(hdrs)) {
            appHeaders.clear();
            appHeaders.add("fizz-appid");
            for (String h : StringUtils.split(hdrs, ',')) {
                appHeaders.add(h.trim());
            }
        }
        WebUtils.setAppHeaders(appHeaders);
        log.info("app headers: " + appHeaders);
    }

    @NacosValue(value = "${custom.header.sign:}", autoRefreshed = true)
    @Value(             "${custom.header.sign:}"                      )
    public void setCustomSignHeaders(String hdrs) {
        if (StringUtils.isNotBlank(hdrs)) {
            signHeaders.clear();
            signHeaders.add("fizz-sign");
            for (String h : StringUtils.split(hdrs, ',')) {
                signHeaders.add(h.trim());
            }
        }
        log.info("sign headers: " + signHeaders);
    }

    @NacosValue(value = "${custom.header.ts:}", autoRefreshed = true)
    @Value(             "${custom.header.ts:}"                      )
    public void setCustomTimestampHeaders(String hdrs) {
        if (StringUtils.isNotBlank(hdrs)) {
            timestampHeaders.clear();
            timestampHeaders.add("fizz-ts");
            for (String h : StringUtils.split(hdrs, ',')) {
                timestampHeaders.add(h.trim());
            }
        }
        log.info("timestamp headers: " + timestampHeaders);
    }

    // TODO: below to X

    @Value("${log.response-body:false}")
    private boolean logResponseBody;

    @Value("${log.headers:x}")
    private String logHeaders;

    private Set<String> logHeaderSet = new HashSet<>();

    @NacosValue(value = "${spring.profiles.active}")
    @Value("${spring.profiles.active}")
    private String profile;

    public Set<String> getLogHeaderSet() {
        return logHeaderSet;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        afterLogResponseBodySet();
        afterLogHeadersSet();
    }

    private void afterLogResponseBodySet() {
        WebUtils.LOG_RESPONSE_BODY = logResponseBody;
        log.info("log response body: " + logResponseBody);
    }

    private void afterLogHeadersSet() {
        logHeaderSet.clear();
        Arrays.stream(StringUtils.split(logHeaders, Constants.Symbol.COMMA)).forEach(h -> {
            logHeaderSet.add(h);
        });
        WebUtils.LOG_HEADER_SET = logHeaderSet;
        log.info("log header list: " + logHeaderSet.toString());
    }

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
                    }
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
