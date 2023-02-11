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

package com.fizzgate.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.fizzgate.context.config.annotation.FizzRefreshScope;
import com.fizzgate.util.Consts;
import com.fizzgate.util.UUIDUtil;
import com.fizzgate.util.WebUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

@FizzRefreshScope
@Component
public class SystemConfig {

    private static final Logger log = LoggerFactory.getLogger(SystemConfig.class);

    public  static  final  String   DEFAULT_GATEWAY_PREFIX            = "/proxy";
    public  static  final  String   DEFAULT_GATEWAY_TEST_PREFIX       = "/_proxytest";
    public  static  final  String   DEFAULT_GATEWAY_TEST              = "_proxytest";
    public  static  final  String   DEFAULT_GATEWAY_TEST_PREFIX0      = "/_proxytest/";

    public  static         boolean  FIZZ_ERR_RESP_HTTP_STATUS_ENABLE  = true;
    public  static         String   FIZZ_ERR_RESP_CODE_FIELD          = "msgCode";
    public  static         String   FIZZ_ERR_RESP_MSG_FIELD           = "message";

    public  static  final  String   FIZZ_DL_ID                        = "fizz-dl-id";
    public  static  final  String   FIZZ_DL_SIGN                      = "fizz-dl-sign";
    public  static  final  String   FIZZ_DL_TS                        = "fizz-dl-ts";
    public  static  final  String   FIZZ_DL_CLIENT                    = "fizz-dl-client";

    public  static  final  String   FIZZ_APP_ID                       = "fizz-appid";
    public  static  final  String   FIZZ_SIGN                         = "fizz-sign";
    public  static  final  String   FIZZ_TIMESTAMP                    = "fizz-ts";

    public  static  final  String   FIZZ_DEDICATED_LINE_SERVER_ENABLE = "fizz.dedicated-line.server.enable";
    public  static  final  String   FIZZ_DEDICATED_LINE_CLIENT_PREFIX = "fizz.dedicated-line.client";
    public  static  final  String   FIZZ_DEDICATED_LINE_CLIENT_ENABLE = "fizz.dedicated-line.client.enable";

    private  String       gatewayPrefix      = DEFAULT_GATEWAY_PREFIX;

    private  List<String> appHeaders         = Stream.of(FIZZ_APP_ID)   .collect(Collectors.toList());
    private  List<String> signHeaders        = Stream.of(FIZZ_SIGN)     .collect(Collectors.toList());
    private  List<String> timestampHeaders   = Stream.of(FIZZ_TIMESTAMP).collect(Collectors.toList());

    private  List<String> proxySetHeaders    = new ArrayList<>();

    private  boolean      aggregateTestAuth  = false;

    @Value("${fizz.md5sign-timestamp-timeliness:300}")
    private int fizzMD5signTimestampTimeliness = 300; // unit: sec

    public int fizzMD5signTimestampTimeliness() {
        return fizzMD5signTimestampTimeliness;
    }

    @Value("${route-timeout:0}")
    private  long         routeTimeout       = 0;

    @Value("${fizz-trace-id.header:X-Trace-Id}")
    private   String      fizzTraceIdHeader;

    @Value("${fizz-trace-id.value-strategy:requestId}")
    private   String      fizzTraceIdValueStrategy;

    @Value("${fizz-trace-id.value-prefix:fizz}")
    private   String      fizzTraceIdValuePrefix;

    @Value("${fizz.error.response.http-status.enable:true}")
    public void setFizzErrRespHttpStatusEnable(boolean fizzErrRespHttpStatusEnable) {
        FIZZ_ERR_RESP_HTTP_STATUS_ENABLE = fizzErrRespHttpStatusEnable;
    }

    @Value("${fizz.error.response.code-field:msgCode}")
    public void setFizzErrRespCodeField(String fizzErrRespCodeField) {
        FIZZ_ERR_RESP_CODE_FIELD = fizzErrRespCodeField;
    }

    @Value("${fizz.error.response.message-field:message}")
    public void setFizzErrRespMsgField(String fizzErrRespMsgField) {
        FIZZ_ERR_RESP_MSG_FIELD = fizzErrRespMsgField;
    }



    @Value("${fizz.dedicated-line.client.request.timeliness:300}")
    private int fizzDedicatedLineClientRequestTimeliness = 300; // unit: sec

    @Value("${fizz.dedicated-line.client.request.timeout:0}")
    private int fizzDedicatedLineClientRequestTimeout = 0;   // mills

    @Value("${fizz.dedicated-line.client.request.retry-count:0}")
    private int fizzDedicatedLineClientRequestRetryCount = 0;

    @Value("${fizz.dedicated-line.client.request.retry-interval:0}")
    private int fizzDedicatedLineClientRequestRetryInterval = 0;   // mills

    @Value("${fizz.dedicated-line.client.request.crypto:true}")
    private boolean fizzDedicatedLineClientRequestCrypto;

    private String fizzDedicatedLineClientId;

    @Value("${fizz.fast-fail-when-registry-center-down:false}")
    private boolean fastFailWhenRegistryCenterDown;

    @Value("${fizz.web-client.x-forwarded-for.enable:true}")
    private boolean fizzWebClientXForwardedForEnable;

    @Value("${fizz.web-client.x-forwarded-for.append-gateway-ip:true}")
    private boolean fizzWebClientXForwardedForAppendGatewayIp;

    public boolean isFizzWebClientXForwardedForAppendGatewayIp() {
        return fizzWebClientXForwardedForAppendGatewayIp;
    }

    public boolean isFizzWebClientXForwardedForEnable() {
        return fizzWebClientXForwardedForEnable;
    }

    public boolean isFastFailWhenRegistryCenterDown() {
        return fastFailWhenRegistryCenterDown;
    }

    public int fizzDedicatedLineClientRequestTimeout() {
        return fizzDedicatedLineClientRequestTimeout;
    }

    public int fizzDedicatedLineClientRequestRetryCount() {
        return fizzDedicatedLineClientRequestRetryCount;
    }

    public int fizzDedicatedLineClientRequestRetryInterval() {
        return fizzDedicatedLineClientRequestRetryInterval;
    }

    public int fizzDedicatedLineClientRequestTimeliness() {
        return fizzDedicatedLineClientRequestTimeliness;
    }

    public boolean fizzDedicatedLineClientRequestCrypto() {
        return fizzDedicatedLineClientRequestCrypto;
    }

    @Value("${fizz.dedicated-line.client.id:}")
    public void setFizzDedicatedLineClientId(String id) {
        if (StringUtils.isBlank(id)) {
            fizzDedicatedLineClientId = UUIDUtil.getUUID();
        } else {
            fizzDedicatedLineClientId = id;
        }
        log.info("fizz dedicated line client id: {}", fizzDedicatedLineClientId);
    }

    public String fizzDedicatedLineClientId() {
        return fizzDedicatedLineClientId;
    }



    public String fizzTraceIdHeader() {
        return fizzTraceIdHeader;
    }

    public String fizzTraceIdValueStrategy() {
        return fizzTraceIdValueStrategy;
    }

    public String fizzTraceIdValuePrefix() {
        return fizzTraceIdValuePrefix;
    }

    public long getRouteTimeout() {
        return routeTimeout;
    }

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

    public List<String> getProxySetHeaders() {
        return proxySetHeaders;
    }

    @Value("${gateway.prefix:/proxy}")
    public void setGatewayPrefix(String gp) {
        gatewayPrefix = gp;
        WebUtils.setGatewayPrefix(gatewayPrefix);
        log.info("gateway prefix: " + gatewayPrefix);
    }

    public String getGatewayPrefix() {
        return gatewayPrefix;
    }

    @Value("${custom.header.appid:}")
    public void setCustomAppHeaders(String hdrs) {
        if (StringUtils.isNotBlank(hdrs)) {
            appHeaders.clear();
            appHeaders.add("fizz-appid");
            for (String h : StringUtils.split(hdrs, ',')) {
                String trim = h.trim();
                if (trim.equals("fizz-appid")) {
                    continue;
                }
                appHeaders.add(trim);
            }
        }
        WebUtils.setAppHeaders(appHeaders);
        log.info("app headers: " + appHeaders);
    }

    @Value("${custom.header.sign:}")
    public void setCustomSignHeaders(String hdrs) {
        if (StringUtils.isNotBlank(hdrs)) {
            signHeaders.clear();
            signHeaders.add("fizz-sign");
            for (String h : StringUtils.split(hdrs, ',')) {
                String trim = h.trim();
                if (trim.equals("fizz-sign")) {
                    continue;
                }
                signHeaders.add(trim);
            }
        }
        WebUtils.setSignHeaders(signHeaders);
        log.info("sign headers: " + signHeaders);
    }

    @Value("${custom.header.ts:}")
    public void setCustomTimestampHeaders(String hdrs) {
        if (StringUtils.isNotBlank(hdrs)) {
            timestampHeaders.clear();
            timestampHeaders.add("fizz-ts");
            for (String h : StringUtils.split(hdrs, ',')) {
                String trim = h.trim();
                if (trim.equals("fizz-ts")) {
                    continue;
                }
                timestampHeaders.add(trim);
            }
        }
        WebUtils.setTimestampHeaders(timestampHeaders);
        log.info("timestamp headers: " + timestampHeaders);
    }

    @Value("${aggregate-test-auth:false}")
    public void setAggregateTestAuth(boolean b) {
        aggregateTestAuth = b;
        log.info("aggregate test auth: " + aggregateTestAuth);
    }

    public boolean isAggregateTestAuth() {
        return aggregateTestAuth;
    }



    // TODO: below to X



    private boolean logResponseBody;

    private String logHeaders;

    private Set<String> logHeaderSet = new HashSet<>();

    @Value("${spring.profiles.active}")
    private String profile;

    public String getProfile() {
        return profile;
    }

    public Set<String> getLogHeaderSet() {
        return logHeaderSet;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        // afterLogResponseBodySet();
        // afterLogHeadersSet();
    }

    private void afterLogResponseBodySet() {
        WebUtils.LOG_RESPONSE_BODY = logResponseBody;
        log.info("log response body: " + logResponseBody);
    }

    private void afterLogHeadersSet() {
        logHeaderSet.clear();
        Arrays.stream(StringUtils.split(logHeaders, Consts.S.COMMA)).forEach(h -> {
            logHeaderSet.add(h);
        });
        if (!fizzTraceIdHeader.equals("X-Trace-Id")) {
            logHeaderSet.add(fizzTraceIdHeader);
        }
        WebUtils.LOG_HEADER_SET = logHeaderSet;
        log.info("log headers: " + logHeaderSet.toString());
    }

    private void updateLogResponseBody(boolean newValue) {
        logResponseBody = newValue;
        this.afterLogResponseBodySet();
    }

    private void updateLogHeaders(String newValue) {
        logHeaders = newValue;
        afterLogHeadersSet();
    }

    @Value("${log.response-body:false}")
    public void setLogResponseBody(boolean logResponseBody) {
        if (this.logResponseBody == logResponseBody) {
            return;
        }
        log.info("log.response-body old: " + this.logResponseBody + ", new: " + logResponseBody);
        this.updateLogResponseBody(logResponseBody);
    }

    @Value("${log.headers:}")
    public void setLogHeaders(String logHeaders) {
        if (ObjectUtils.nullSafeEquals(this.logHeaders, logHeaders)) {
            return;
        }
        log.info("log.headers old: " + this.logHeaders + ", new: " + logHeaders);
        this.updateLogHeaders(logHeaders);
    }
}
