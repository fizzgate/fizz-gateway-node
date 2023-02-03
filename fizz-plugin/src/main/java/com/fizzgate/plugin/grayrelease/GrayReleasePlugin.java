/*
 *  Copyright (C) 2021 the original author or authors.
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

package com.fizzgate.plugin.grayrelease;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fizzgate.plugin.FizzPluginFilterChain;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.requestbody.RequestBodyPlugin;
import com.fizzgate.proxy.Route;
import com.fizzgate.spring.web.server.ext.FizzServerWebExchangeDecorator;
import com.fizzgate.util.*;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author hongqiaowei
 */

@Component(GrayReleasePlugin.GRAY_RELEASE_PLUGIN)
public class GrayReleasePlugin extends RequestBodyPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrayReleasePlugin.class);

    public  static final String GRAY_RELEASE_PLUGIN = "GrayReleasePlugin";

    private static final String triggerCondition = "triggerCondition";
    private static final String routeType        = "routeType";
    private static final String routeConfig      = "routeConfig";
    private static final String routeConfigMap   = "routeConfigMap";
    private static final String method           = "method";
    private static final String path             = "path";
    private static final String contentType      = "contentType";
    private static final String body             = "body";
    private static final String form             = "form";
    private static final String cookie           = "cookie";
    private static final String header           = "header";
    private static final String query            = "query";
    private static final String client           = "client";
    private static final String ip               = "ip";

    private static class OgnlRoot extends HashMap<String, Object> {

        public double random() {
            return Math.random();
        }

        public boolean exist(String key) {
            String[] keys = StringUtils.split(key, Consts.S.DOT);
            Map<String, Object> m = this;
            int keyLen = keys.length;
            for (int i = 0; i < keyLen; i++) {
                String k = keys[i];
                if (m.containsKey(k)) {
                    Object obj = m.get(k);
                    if (obj instanceof Map) {
                        m = (Map<String, Object>) obj;
                    } else if (i + 1 != keyLen) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }

        public boolean matches(String key, String regex) throws OgnlException {
            String value = (String) Ognl.getValue(key, this);
            if (value == null) {
                return false;
            }
            return value.matches(regex);
        }

        public String jwtClaim(String name) {
            Map<String, Object> headerMap = (Map<String, Object>) get(GrayReleasePlugin.header);
            if (headerMap == null) {
                return null;
            } else {
                String token = (String) headerMap.get(HttpHeaders.AUTHORIZATION.toLowerCase());
                if (StringUtils.isBlank(token)) {
                    return null;
                } else if (token.length() > 7 && token.substring(0, 7).equalsIgnoreCase("Bearer ")) {
                    token = token.substring(7);
                }
                DecodedJWT jwt = JWT.decode(token);
                Claim claim = jwt.getClaim(name);
                if (claim == null) {
                    return null;
                }
                return claim.asString();
            }
        }

        public boolean clientIpInRange(String range) throws AddressStringException {
            Map<String, Object> cli = (Map<String, Object>) get(client);
            if (cli == null) {
                return false;
            } else {
                String pi = (String) cli.get(ip);
                if (pi == null) {
                    return false;
                } else {
                    return ipInRange(pi, range);
                }
            }
        }

        public boolean clientIpInRange(String rangeStartIp, String rangeEndIp) throws AddressStringException {
            Map<String, Object> cli = (Map<String, Object>) get(client);
            if (cli == null) {
                return false;
            } else {
                String pi = (String) cli.get(ip);
                if (pi == null) {
                    return false;
                } else {
                    return ipInRange(pi, rangeStartIp, rangeEndIp);
                }
            }
        }

        public boolean ipInRange(String ip, String range) throws AddressStringException {
            IPAddress ipAddress = new IPAddressString(ip).toAddress();
            IPAddress rangeAddress = new IPAddressString(range).getAddress();
            return rangeAddress.contains(ipAddress);
        }

        public boolean ipInRange(String ip, String rangeStartIp, String rangeEndIp) throws AddressStringException {
            IPAddress startIPAddress = new IPAddressString(rangeStartIp).getAddress();
            IPAddress endIPAddress = new IPAddressString(rangeEndIp).getAddress();
            IPAddressSeqRange ipRange = startIPAddress.spanWithRange(endIPAddress);
            IPAddress ipAddress = new IPAddressString(ip).toAddress();
            return ipRange.contains(ipAddress);
        }

        public String toString() {
            return JacksonUtils.writeValueAsString(this);
        }
    }

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config) {
        String traceId = WebUtils.getTraceId(exchange);
        ThreadContext.put(Consts.TRACE_ID, traceId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("gray release plugin config: {}", JacksonUtils.writeValueAsString(config));
        }

        String tc = (String) config.get(triggerCondition);
        Object ognlRoot = request2ognlContext(exchange);
        Boolean conditionMatch = false;
        try {
            conditionMatch = (Boolean) Ognl.getValue(tc, ognlRoot);
        } catch (OgnlException e) {
            LOGGER.error("calc condition expression {} with context {}", tc, ognlRoot, e);
            throw new RuntimeException(e);
        }
        if (conditionMatch) {
            Route route = WebUtils.getRoute(exchange);
            changeRoute(exchange, route, config);
            if (route.type == ApiConfig.Type.DIRECT_RESPONSE) {
                HttpHeaders hdrs = new HttpHeaders();
                hdrs.setContentType(route.contentType);
                return WebUtils.response(exchange, HttpStatus.OK, hdrs, route.body);
            } else {
                exchange.getAttributes().put(WebUtils.IGNORE_PLUGIN, Consts.S.EMPTY);
            }
        }
        return FizzPluginFilterChain.next(exchange);
    }

    private Object request2ognlContext(ServerWebExchange exchange) {
        OgnlRoot ognlRoot = new OgnlRoot();
        ServerHttpRequest request = exchange.getRequest();

        ognlRoot.put(method, request.getMethodValue().toLowerCase());
        ognlRoot.put(path, WebUtils.getClientReqPath(exchange));

        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (!queryParams.isEmpty()) {
            Map<String, Object> queryMap = new HashMap<>();
            queryParams.forEach(
                                    (name, values) -> {
                                        if (CollectionUtils.isEmpty(values)) {
                                            queryMap.put(name, null);
                                        } else if (values.size() > 1) {
                                            queryMap.put(name, values);
                                        } else {
                                            queryMap.put(name, values.get(0));
                                        }
                                    }
                       );
            ognlRoot.put(query, queryMap);
        }

        HttpHeaders headers = request.getHeaders();
        if (!headers.isEmpty()) {
            Map<String, Object> headerMap = new HashMap<>();
            headers.forEach(
                                (nm, values) -> {
                                    String name = nm.toLowerCase();
                                    if (CollectionUtils.isEmpty(values)) {
                                        headerMap.put(name, null);
                                    } else if (values.size() > 1) {
                                        headerMap.put(name, values);
                                    } else {
                                        headerMap.put(name, values.get(0));
                                    }
                                }
                   );
            ognlRoot.put(header, headerMap);
        }

        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (!CollectionUtils.isEmpty(cookies)) {
            Map<String, Object> cookieMap = new HashMap<>();
            cookies.forEach(
                                (name, values) -> {
                                    if (CollectionUtils.isEmpty(values)) {
                                        cookieMap.put(name, null);
                                    } else if (values.size() > 1) {
                                        List<String> lst = new ArrayList<>(values.size());
                                        for (HttpCookie value : values) {
                                            lst.add(value.getValue());
                                        }
                                        cookieMap.put(name, lst);
                                    } else {
                                        cookieMap.put(name, values.get(0).getValue());
                                    }
                                }
                   );
            ognlRoot.put(cookie, cookieMap);
        }

        MediaType reqContentType = request.getHeaders().getContentType();
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(reqContentType)) {
            exchange.getFormData()
                    .map(
                            formData -> {
                                if (formData == FizzServerWebExchangeDecorator.EMPTY_FORM_DATA) {
                                    return null;
                                } else {
                                    Map<String, Object> formMap = new HashMap<>();
                                    formData.forEach(
                                            (name, values) -> {
                                                if (CollectionUtils.isEmpty(values)) {
                                                    formMap.put(name, null);
                                                } else if (values.size() > 1) {
                                                    formMap.put(name, values);
                                                } else {
                                                    formMap.put(name, values.get(0));
                                                }
                                            }
                                    );
                                    ognlRoot.put(form, formMap);
                                    return formMap;
                                }
                            }
                    )
                    .subscribe();
        } else if (MediaType.APPLICATION_JSON.isCompatibleWith(reqContentType)) {
            request.getBody()
                   .single()
                   .map(
                           bodyDataBuffer -> {
                               if (bodyDataBuffer == NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                   return ReactorUtils.NULL;
                               } else {
                                   String json = bodyDataBuffer.toString(StandardCharsets.UTF_8).trim();
                                   if (LOGGER.isDebugEnabled()) {
                                       LOGGER.debug("request {} body: {}", request.getId(), json);
                                   }
                                   if (json.charAt(0) == Consts.S.LEFT_SQUARE_BRACKET) {
                                       List<Object> bodyMap = JacksonUtils.readValue(json, new TypeReference<List<Object>>(){});
                                       ognlRoot.put(body, bodyMap);
                                   } else {
                                       Map<String, Object> bodyMap = JacksonUtils.readValue(json, new TypeReference<Map<String, Object>>(){});
                                       ognlRoot.put(body, bodyMap);
                                   }
                                   return ReactorUtils.NULL;
                               }
                           }
                   )
                   .subscribe();
        }

        String originIp = WebUtils.getOriginIp(exchange);
        if (originIp != null) {
            Map<String, Object> clientMap = new HashMap<>();
            clientMap.put(ip, originIp);
            ognlRoot.put(client, clientMap);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("request {} ognl root: {}", request.getId(), JacksonUtils.writeValueAsString(ognlRoot));
        }

        return ognlRoot;
    }

    private void changeRoute(ServerWebExchange exchange, Route route, Map<String, Object> pluginConfig) {
        byte rt = ((Integer) pluginConfig.get(routeType)).byteValue();
        route.type = rt;
        Map<String, String> newRouteConfig = (Map<String, String>) pluginConfig.get(routeConfigMap);
        if (newRouteConfig == null && rt != ApiConfig.Type.DIRECT_RESPONSE) {
            newRouteConfig = routeConfig2map((String) pluginConfig.get(routeConfig));
            pluginConfig.put(routeConfigMap, newRouteConfig);
            pluginConfig.remove(routeConfig);
        }
        if (rt == ApiConfig.Type.SERVICE_DISCOVERY) {
            changeServiceDiscoveryRoute(exchange, route, newRouteConfig);
        } else if (rt == ApiConfig.Type.REVERSE_PROXY) {
            changeReverseProxyRoute(exchange, pluginConfig, route, newRouteConfig);
        } else if (rt == ApiConfig.Type.SERVICE_AGGREGATE) {
            changeAggregateRoute(exchange, route, newRouteConfig);
        } else {
            String ct = (String) pluginConfig.get(contentType);
            String b  = (String) pluginConfig.get(body);
            route.contentType(MediaType.valueOf(ct))
                 .body(b);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("route is changed: {}", route);
        }
    }

    private void changeServiceDiscoveryRoute(ServerWebExchange exchange, Route route, Map<String, String> newRouteConfig) {
        String type = newRouteConfig.get("type");
        String service = newRouteConfig.get("serviceName");
        if (StringUtils.isNotBlank(service)) {
            route.backendService = service.trim();
        }
        String timeout = newRouteConfig.get("timeout");
        if (StringUtils.isNotBlank(timeout)) {
            route.timeout(Long.parseLong(timeout.trim()));
        }
        if (type.equals("http")) {
            String registry = newRouteConfig.get("registry");
            if (StringUtils.isNotBlank(registry)) {
                route.registryCenter = registry.trim();
            }
            String method = newRouteConfig.get("methodName");
            if (StringUtils.isNotBlank(method)) {
                route.method(HttpMethod.resolve(method.trim()));
            }
            String path = newRouteConfig.get("path");
            if (StringUtils.isNotBlank(path)) {
                route.backendPath = UrlTransformUtils.transform(route.path, path.trim(), WebUtils.getClientReqPath(exchange));
            }
            String qry = newRouteConfig.get("query");
            if (StringUtils.isNotBlank(qry)) {
                String reqQry = exchange.getRequest().getURI().getQuery();
                if (StringUtils.isBlank(reqQry)) {
                    route.query = qry.trim();
                } else {
                    route.query = reqQry + Consts.S.AND + qry.trim();
                }
            }
            String headerStr = newRouteConfig.get("header");
            if (StringUtils.isNotBlank(headerStr)) {
                Map<String, String> headers = headerStr2map(headerStr);
                WebUtils.appendHeaders(exchange, headers);
            }
            String retryCount = newRouteConfig.get("retryCount");
            if (StringUtils.isNotBlank(retryCount)) {
                route.retryCount(Integer.parseInt(retryCount.trim()));
            }
            String retryInterval = newRouteConfig.get("retryInterval");
            if (StringUtils.isNotBlank(retryInterval)) {
                route.retryInterval(Long.parseLong(retryInterval.trim()));
            }
        } else {
            route.type = ApiConfig.Type.DUBBO;
            String method = newRouteConfig.get("methodName");
            if (StringUtils.isNotBlank(method)) {
                route.rpcMethod(method.trim());
            }
            String version = newRouteConfig.get("version");
            if (StringUtils.isNotBlank(version)) {
                route.rpcVersion(version.trim());
            }
            String group = newRouteConfig.get("group");
            if (StringUtils.isNotBlank(group)) {
                route.rpcGroup(group.trim());
            }
            String paramTypes = newRouteConfig.get("paramTypes");
            if (StringUtils.isNotBlank(paramTypes)) {
                route.rpcParamTypes(paramTypes.trim());
            }
        }
    }

    private void changeReverseProxyRoute(ServerWebExchange exchange, Map<String, Object> pluginConfig, Route route, Map<String, String> newRouteConfig) {
        List<String> httpHostPorts = (List<String>) pluginConfig.get("httpHostPorts");
        if (httpHostPorts == null) {
            String httpHostPortStr = newRouteConfig.get("serviceName");
            if (StringUtils.isBlank(httpHostPortStr)) {
                httpHostPorts = WebUtils.getApiConfig(exchange).httpHostPorts;
            } else {
                String[] httpHostPortArr = StringUtils.split(httpHostPortStr, Consts.S.COMMA);
                for (int i = 0; i < httpHostPortArr.length; i++) {
                    httpHostPortArr[i] = httpHostPortArr[i].trim();
                }
                httpHostPorts = Arrays.asList(httpHostPortArr);
            }
            pluginConfig.put("httpHostPorts", httpHostPorts);
            newRouteConfig.remove("serviceName");
        }
        int counter = (int) pluginConfig.getOrDefault("counter", -1);
        counter++;
        if (counter < 0) {
            counter = Math.abs(counter);
        }
        String hostPort = httpHostPorts.get(
                                                counter % httpHostPorts.size()
                                           );
        route.nextHttpHostPort(hostPort);
        pluginConfig.put("counter", counter);

        String method = newRouteConfig.get("methodName");
        if (StringUtils.isNotBlank(method)) {
            route.method(HttpMethod.resolve(method.trim()));
        }

        String path = newRouteConfig.get("path");
        if (StringUtils.isNotBlank(path)) {
            route.backendPath = UrlTransformUtils.transform(route.path, path.trim(), WebUtils.getClientReqPath(exchange));
        }

        String qry = newRouteConfig.get("query");
        if (StringUtils.isNotBlank(qry)) {
            String reqQry = exchange.getRequest().getURI().getQuery();
            if (StringUtils.isBlank(reqQry)) {
                route.query = qry.trim();
            } else {
                route.query = reqQry + Consts.S.AND + qry.trim();
            }
        }

        String headerStr = newRouteConfig.get("header");
        if (StringUtils.isNotBlank(headerStr)) {
            Map<String, String> headers = headerStr2map(headerStr);
            WebUtils.appendHeaders(exchange, headers);
        }

        String timeout = newRouteConfig.get("timeout");
        if (StringUtils.isNotBlank(timeout)) {
            route.timeout(Long.parseLong(timeout.trim()));
        }

        String retryCount = newRouteConfig.get("retryCount");
        if (StringUtils.isNotBlank(retryCount)) {
            route.retryCount(Integer.parseInt(retryCount.trim()));
        }

        String retryInterval = newRouteConfig.get("retryInterval");
        if (StringUtils.isNotBlank(retryInterval)) {
            route.retryInterval(Long.parseLong(retryInterval.trim()));
        }
    }

    private void changeAggregateRoute(ServerWebExchange exchange, Route route, Map<String, String> newRouteConfig) {
        String service = newRouteConfig.get("serviceName");
        if (StringUtils.isNotBlank(service)) {
            route.backendService = service.trim();
            WebUtils.setBackendService(exchange, route.backendService);
        }
        String path = newRouteConfig.get("path");
        if (StringUtils.isNotBlank(path)) {
            route.backendPath = UrlTransformUtils.transform(route.path, path.trim(), WebUtils.getClientReqPath(exchange));
            WebUtils.setBackendPath(exchange, route.backendPath);
        }
        String headerStr = newRouteConfig.get("header");
        if (StringUtils.isNotBlank(headerStr)) {
            Map<String, String> headers = headerStr2map(headerStr);
            WebUtils.appendHeaders(exchange, headers);
        }
    }

    private Map<String, String> routeConfig2map(String config) {
        Map<String, String> result = new HashMap<>();
        String[] lines = StringUtils.split(config, Consts.S.LF);
        for (String line : lines) {
            int colonIdx = line.indexOf(Consts.S.COLON);
            result.put(line.substring(0, colonIdx).trim(), line.substring(colonIdx + 1).trim());
        }
        return result;
    }

    private Map<String, String> headerStr2map(String headerStr) {
        String[] hvs = StringUtils.split(headerStr, Consts.S.COMMA);
        Map<String, String> headerMap = new HashMap<>(hvs.length, 1);
        for (String hv : hvs) {
            int eqIdx = hv.indexOf(Consts.S.EQUAL);
            String h = hv.substring(0, eqIdx).trim();
            String v = hv.substring(eqIdx + 1).trim();
            headerMap.put(h, v);
        }
        return headerMap;
    }
}
