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

package we.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.filter.FilterResult;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.AuthPluginFilter;
import we.proxy.Route;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

public abstract class WebUtils {

    // TODO: don't log in this class
    private  static  final  Logger       log                          = LoggerFactory.getLogger(WebUtils.class);

    private  static  final  String       clientService                = "cs@";

    private  static  final  String       xForwardedFor                = "X-FORWARDED-FOR";

    private  static  final  String       unknown                      = "unknown";

    private  static  final  String       loopBack                     = "127.0.0.1";

    private  static  final  String       binaryAddress                = "0:0:0:0:0:0:0:1";

    private  static  final  String       directResponse               = "dr@";

    private  static  final  String       response                     = " response ";

    private  static  final  String       originIp                     = "oi@";

    private  static  final  String       clientRequestPath            = "crp@";

    private  static  final  String       clientRequestPathPrefix      = "crpp@";

    private  static  final  String       clientRequestQuery           = "crq@";

    private  static         String       gatewayPrefix                = SystemConfig.DEFAULT_GATEWAY_PREFIX;

    private  static         List<String> appHeaders                   = Stream.of(SystemConfig.FIZZ_APP_ID)   .collect(Collectors.toList());
    
    private  static         List<String> signHeaders                  = Stream.of(SystemConfig.FIZZ_SIGN)     .collect(Collectors.toList());

    private  static         List<String> timestampHeaders             = Stream.of(SystemConfig.FIZZ_TIMESTAMP).collect(Collectors.toList());

    public   static  final  String       TRACE_ID                     = "traid@";

    public   static  final  String       BACKEND_SERVICE              = "bs@";

    public   static  final  String       FILTER_CONTEXT               = "fc@";

    public   static  final  String       APPEND_HEADERS               = "ahs@";

    public   static  final  String       PREV_FILTER_RESULT           = "pfr@";

    public   static  final  String       BACKEND_PATH                 = "bp@";

    public   static  final  String       ROUTE                        = "rout@";

    public   static         boolean      LOG_RESPONSE_BODY            = false;

    public   static         Set<String>  LOG_HEADER_SET               = Collections.emptySet();

    public   static  final  String       ADMIN_REQUEST                = "ar@";

    public   static  final  String       FIZZ_REQUEST                 = "fr@";

    public   static  final  String       FAV_REQUEST                  = "fa@";

    public   static  final  String       BODY_ENCRYPT                 = "b-ecyt";

    public   static  final  String       ORIGINAL_ERROR               = "origerr@";


    private WebUtils() {
    }

    public static boolean isFavReq(ServerWebExchange exchange) {
        return exchange.getAttribute(FAV_REQUEST) != null;
    }

    public static boolean isAdminReq(ServerWebExchange exchange) {
        return exchange.getAttribute(ADMIN_REQUEST) != null;
    }

    public static boolean isFizzReq(ServerWebExchange exchange) {
        return exchange.getAttribute(FIZZ_REQUEST) != null;
    }

    public static void setGatewayPrefix(String p) {
        gatewayPrefix = p;
    }

    public static void setAppHeaders(List<String> hdrs) {
        appHeaders = hdrs;
    }
    
    public static void setSignHeaders(List<String> hdrs) {
    	signHeaders = hdrs;
    }
    
    public static void setTimestampHeaders(List<String> hdrs) {
    	timestampHeaders = hdrs;
    }

    public static String getHeaderValue(ServerWebExchange exchange, String header) {
        return exchange.getRequest().getHeaders().getFirst(header);
    }

    public static List<String> getHeaderValues(ServerWebExchange exchange, String header) {
        return exchange.getRequest().getHeaders().get(header);
    }

    public static boolean isDedicatedLineRequest(ServerWebExchange exchange) {
        String v = exchange.getRequest().getHeaders().getFirst(SystemConfig.FIZZ_DL_ID);
        return v != null;
    }

    public static String getAppId(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        for (int i = 0; i < appHeaders.size(); i++) {
            String v = headers.getFirst(appHeaders.get(i));
            if (v != null) {
                return v;
            }
        }
        return null;
    }
    
    public static String getTimestamp(ServerWebExchange exchange) {
    	HttpHeaders headers = exchange.getRequest().getHeaders();
        for (int i = 0; i < timestampHeaders.size(); i++) {
            String v = headers.getFirst(timestampHeaders.get(i));
            if (v != null) {
                return v;
            }
        }
        return null;
    }
    
    public static String getSign(ServerWebExchange exchange) {
    	HttpHeaders headers = exchange.getRequest().getHeaders();
        for (int i = 0; i < signHeaders.size(); i++) {
            String v = headers.getFirst(signHeaders.get(i));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    public static String getDedicatedLineId(ServerWebExchange exchange) {
        return getHeaderValue(exchange, SystemConfig.FIZZ_DL_ID);
    }

    public static String getDedicatedLineTimestamp(ServerWebExchange exchange) {
        return getHeaderValue(exchange, SystemConfig.FIZZ_DL_TS);
    }

    public static String getDedicatedLineSign(ServerWebExchange exchange) {
        return getHeaderValue(exchange, SystemConfig.FIZZ_DL_SIGN);
    }
 
    public static String getClientService(ServerWebExchange exchange) {
        String svc = exchange.getAttribute(clientService);
        if (svc == null) {
            String p = exchange.getRequest().getPath().value();
            int secFS = p.indexOf(Consts.S.FORWARD_SLASH, 1);
            if (StringUtils.isBlank(gatewayPrefix) || Consts.S.FORWARD_SLASH_STR.equals(gatewayPrefix)) {
                svc = p.substring(1, secFS);
            } else {
                String prefix = p.substring(0, secFS);
                if (gatewayPrefix.equals(prefix) || SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX.equals(prefix)) {
                    int trdFS = p.indexOf(Consts.S.FORWARD_SLASH, secFS + 1);
                    svc = p.substring(secFS + 1, trdFS);
                } else {
                    throw Utils.runtimeExceptionWithoutStack("wrong prefix " + prefix);
                }
            }
            exchange.getAttributes().put(clientService, svc);
        }
        return svc;
    }

    public static void setBackendService(ServerWebExchange exchange, String service) {
        exchange.getAttributes().put(BACKEND_SERVICE, service);
    }

    public static String getBackendService(ServerWebExchange exchange) {
        return exchange.getAttribute(BACKEND_SERVICE);
    }

    public static byte getApiConfigType(ServerWebExchange exchange) {
        ApiConfig ac = getApiConfig(exchange);
        if (ac == null) {
            return ApiConfig.Type.UNDEFINED;
        } else {
            return ac.type;
        }
    }

    public static ApiConfig getApiConfig(ServerWebExchange exchange) {
        Result<ApiConfig> authRes = (Result<ApiConfig>) getFilterResultDataItem(exchange, AuthPluginFilter.AUTH_PLUGIN_FILTER, AuthPluginFilter.RESULT);
        if (authRes == null) {
            return null;
        }
        return authRes.data;
    }

    public static Route getRoute(ServerWebExchange exchange) {
        return exchange.getAttribute(ROUTE);
    }

    public static Mono<Void> response(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String body) {
        return response(exchange.getResponse(), status, headers, body);
    }

    public static Mono<Void> response(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, DataBuffer body) {
        if (clientResp.isCommitted()) {
            String s = body.toString(StandardCharsets.UTF_8);
            String msg = "try to response: " + s + ", but server http response is committed and it's status: " + clientResp.getStatusCode();
            log.warn(msg);
            return Mono.error(Utils.runtimeExceptionWithoutStack(msg));
        }
        if (status != null) {
            clientResp.setStatusCode(status);
        }
        if (headers != null) {
            headers.forEach(  (h, vs) -> {clientResp.getHeaders().addAll(h, vs);}  );
        }
        if (body == null) {
            body = NettyDataBufferUtils.EMPTY_DATA_BUFFER;
        }
        return clientResp.writeWith(Mono.just(body));
    }

    public static Mono<Void> response(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, ByteBuffer body) {
        DataBuffer dataBuffer = null;
        if (body != null) {
            dataBuffer = clientResp.bufferFactory().wrap(body);
        }
        return response(clientResp, status, headers, dataBuffer);
    }

    public static Mono<Void> response(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, byte[] body) {
        DataBuffer dataBuffer = null;
        if (body != null) {
            dataBuffer = clientResp.bufferFactory().wrap(body);
        }
        return response(clientResp, status, headers, dataBuffer);
    }

    public static Mono<Void> response(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, String body) {
        DataBuffer dataBuffer = null;
        if (body != null) {
            dataBuffer = clientResp.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        }
        return response(clientResp, status, headers, dataBuffer);
    }

    public static Mono<Void> responseJson(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, Object body) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = null;
        if (body != null) {
            bytes = JacksonUtils.writeValueAsBytes(body);
        }
        return response(exchange.getResponse(), status, headers, bytes);
    }

    public static Mono<Void> responseJson(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String json) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return response(exchange, status, headers, json);
    }

    private static void bind(ServerWebExchange exchange, String filter, FilterResult fr) {
        Map<String, FilterResult> fc = getFilterContext(exchange);
        fc.put(filter, fr);
        fc.put(PREV_FILTER_RESULT, fr);
    }

    public static String getClientReqPath(ServerWebExchange exchange) {
        String p = exchange.getAttribute(clientRequestPath);
        if (p == null) {
            p = exchange.getRequest().getPath().value();
            int secFS = p.indexOf(Consts.S.FORWARD_SLASH, 1);
            if (StringUtils.isBlank(gatewayPrefix) || Consts.S.FORWARD_SLASH_STR.equals(gatewayPrefix)) {
                p = p.substring(secFS);
            } else {
                String prefix = p.substring(0, secFS);
                if (gatewayPrefix.equals(prefix) || SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX.equals(prefix)) {
                    int trdFS = p.indexOf(Consts.S.FORWARD_SLASH, secFS + 1);
                    p = p.substring(trdFS);
                } else {
                    throw Utils.runtimeExceptionWithoutStack("wrong prefix " + prefix);
                }
            }
            exchange.getAttributes().put(clientRequestPath, p);
        }
        return p;
    }

    public static void setBackendPath(ServerWebExchange exchange, String path) {
        exchange.getAttributes().put(BACKEND_PATH, path);
    }

    public static String getBackendPath(ServerWebExchange exchange) {
        return exchange.getAttribute(BACKEND_PATH);
    }

    public static String getClientReqPathPrefix(ServerWebExchange exchange) {
        String prefix = exchange.getAttribute(clientRequestPathPrefix);
        if (prefix == null) {
            if (StringUtils.isBlank(gatewayPrefix) || Consts.S.FORWARD_SLASH_STR.equals(gatewayPrefix)) {
                prefix = Consts.S.FORWARD_SLASH_STR;
            } else {
                String path = exchange.getRequest().getPath().value();
                int secFS = path.indexOf(Consts.S.FORWARD_SLASH, 1);
                prefix = path.substring(0, secFS);
                if (gatewayPrefix.equals(prefix) || SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX.equals(prefix)) {
                    prefix = prefix + Consts.S.FORWARD_SLASH;
                } else {
                    throw Utils.runtimeExceptionWithoutStack("wrong prefix " + prefix);
                }
            }
            exchange.getAttributes().put(clientRequestPathPrefix, prefix);
        }
        return prefix;
    }

    public static String getClientReqQuery(ServerWebExchange exchange) {
        String qry = exchange.getAttribute(clientRequestQuery);
        if (qry != null && StringUtils.EMPTY.equals(qry)) {
            return null;
        } else {
            if (qry == null) {
                URI uri = exchange.getRequest().getURI();
                qry = uri.getQuery();
                if (qry == null) {
                    exchange.getAttributes().put(clientRequestQuery, StringUtils.EMPTY);
                } else {
                    if (StringUtils.indexOfAny(qry, Consts.S.LEFT_BRACE, Consts.S.FORWARD_SLASH, Consts.S.HASH) > 0) {
                        qry = uri.getRawQuery();
                    }
                    exchange.getAttributes().put(clientRequestQuery, qry);
                }
            }
            return qry;
        }
    }

    public static String getClientReqPathQuery(ServerWebExchange exchange) {
        String pathQry = getClientReqPath(exchange);
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        if (!queryParams.isEmpty()) {
            String qry = toQueryString(queryParams);
            pathQry = pathQry + Consts.S.QUESTION + qry;
        }
        return pathQry;
    }

    public static Map<String, List<String>> getClientReqPathQueryTemplate(ServerWebExchange exchange) {
        String pathQry = getClientReqPath(exchange);
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        if (queryParams.isEmpty()) {
            return Collections.singletonMap(pathQry, Collections.emptyList());
        } else {
            Map<String, List<String>> queryStringTemplate = toQueryStringTemplate(queryParams);
            Map.Entry<String, List<String>> entry = queryStringTemplate.entrySet().iterator().next();
            pathQry = pathQry + Consts.S.QUESTION + entry.getKey();
            return Collections.singletonMap(pathQry, entry.getValue());
        }
    }

    public static String appendQuery(String path, ServerWebExchange exchange) {
        String qry = getClientReqQuery(exchange);
        if (qry != null) {
            return path + Consts.S.QUESTION + qry;
        }
        return path;
    }

    public static Map<String, String> appendHeader(ServerWebExchange exchange, String name, String value) {
        Map<String, String> hdrs = getAppendHeaders(exchange);
        hdrs.put(name, value);
        return hdrs;
    }

    public static Map<String, String> getAppendHeaders(ServerWebExchange exchange) {
        return (Map<String, String>) exchange.getAttribute(APPEND_HEADERS);
    }

    public static HttpHeaders mergeAppendHeaders(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        Map<String, String> appendHeaders = getAppendHeaders(exchange);
        if (appendHeaders.isEmpty()) {
            return req.getHeaders();
        }
        HttpHeaders hdrs = new HttpHeaders();
        req.getHeaders().forEach(
                (h, vs) -> {
                    hdrs.addAll(h, vs);
                }
        );
        appendHeaders.forEach(
                (h, v) -> {
                    List<String> vs = hdrs.get(h);
                    if (vs != null && !vs.isEmpty()) {
                        vs.clear();
                        vs.add(v);
                    } else {
                        hdrs.add(h, v);
                    }
                }
        );
        return hdrs;
    }

    public static void request2stringBuilder(ServerWebExchange exchange, StringBuilder b) {
        ServerHttpRequest req = exchange.getRequest();
        request2stringBuilder(WebUtils.getTraceId(exchange), req.getMethod(), req.getURI().toString(), req.getHeaders(), null, b);
    }

    public static void request2stringBuilder(String traceId, HttpMethod method, String uri, HttpHeaders headers, Object body, StringBuilder b) {
        b.append(traceId).append(Consts.S.SPACE).append(method).append(Consts.S.SPACE).append(uri);
        if (headers != null) {
            final boolean[] f = {false};
            LOG_HEADER_SET.forEach(
                    h -> {
                        String v = headers.getFirst(h);
                        if (v != null) {
                            if (!f[0]) {
                                b.append(Consts.S.LINE_SEPARATOR);
                                f[0] = true;
                            }
                            Utils.addTo(b, h, Consts.S.EQUAL, v, Consts.S.TWO_SPACE_STR);
                        }
                    }
            );
        }
        // body to b
    }

    public static void response2stringBuilder(String traceId, ClientResponse clientResponse, StringBuilder b) {
        b.append(traceId).append(response).append(clientResponse.statusCode());
        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
        final boolean[] f = {false};
        LOG_HEADER_SET.forEach(
                h -> {
                    String v = headers.getFirst(h);
                    if (v != null) {
                        if (!f[0]) {
                            b.append(Consts.S.LINE_SEPARATOR);
                            f[0] = true;
                        }
                        Utils.addTo(b, h, Consts.S.EQUAL, v, Consts.S.TWO_SPACE_STR);
                    }
                }
        );
        // body to b
    }

    private static Mono<Void> responseError(ServerWebExchange exchange, String filter, int code, String msg, Throwable t, boolean bindContext) {
        // Mono<DataBuffer> reqBodyMono = getRequestBody(exchange);
        // final DataBuffer[] reqBody = {null};
        // if (reqBodyMono != null) {
        //     reqBodyMono.subscribe(
        //             db -> {
        //                 reqBody[0] = db;
        //                 DataBufferUtils.retain(reqBody[0]);
        //             }
        //     );
        // }

        exchange.getResponse().getHeaders().set(BODY_ENCRYPT, Consts.S.FALSE0);

        String traceId = getTraceId(exchange);
        // Schedulers.parallel().schedule(() -> {
        StringBuilder b = ThreadContext.getStringBuilder();
        request2stringBuilder(exchange, b);
        // if (reqBody[0] != null) {
        //     DataBufferUtils.release(reqBody[0]);
        // }
        b.append(Consts.S.LINE_SEPARATOR);
        b.append(filter).append(Consts.S.SPACE).append(code).append(Consts.S.SPACE).append(msg);
        org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
        if (t == null) {
            // log.error(b.toString(), LogService.BIZ_ID, traceId);
            log.error(b.toString());
        } else {
            // log.error(b.toString(), LogService.BIZ_ID, traceId, t);
            log.error(b.toString(), t);
            Throwable[] suppressed = t.getSuppressed();
            if (suppressed != null && suppressed.length != 0) {
                log.error(StringUtils.EMPTY, suppressed[0]);
            }
        }
        // });
        if (filter != null) {
            if (t == null) {
                transmitFailFilterResult(exchange, filter);
            } else {
                transmitFailFilterResult(exchange, filter, t);
            }
        }
        HttpStatus s = HttpStatus.OK;
        if (SystemConfig.FIZZ_ERR_RESP_HTTP_STATUS_ENABLE) {
            s = HttpStatus.resolve(code);
        }
        if (bindContext) {
            return buildJsonDirectResponseAndBindContext(exchange, s, null, jsonRespBody(code, msg, traceId));
        } else {
            return buildJsonDirectResponse(exchange, s, null, jsonRespBody(code, msg, traceId));
        }
    }

    public static Mono<Void> responseError(ServerWebExchange exchange, int code, String msg) {
        return responseError(exchange, null, code, msg, null, false);
    }

    public static Mono<Void> responseError(ServerWebExchange exchange, String reporter, int code, String msg, Throwable t) {
        return responseError(exchange, reporter, code, msg, t, false);
    }

    public static String getOriginIp(ServerWebExchange exchange) {
        String ip = exchange.getAttribute(originIp);
        if (ip == null) {
            ServerHttpRequest req = exchange.getRequest();
            String v = req.getHeaders().getFirst(xForwardedFor);
            if (StringUtils.isBlank(v)) {
                ip = req.getRemoteAddress().getAddress().getHostAddress();
            } else {
                ip = StringUtils.split(v, Consts.S.COMMA)[0].trim();
                if (ip.equalsIgnoreCase(unknown)) {
                    ip = req.getRemoteAddress().getAddress().getHostAddress();
                } else if (ip.equals(binaryAddress)) {
                    ip = loopBack;
                }
            }
            exchange.getAttributes().put(originIp, ip);
        }
        return ip;
    }

    public static String getTraceId(ServerWebExchange exchange) {
        String id = exchange.getAttribute(TRACE_ID);
        if (id == null) {
            id = exchange.getRequest().getId();
        }
        return id;
    }

    public static void traceDebug(Logger log, Function<Boolean, String> messageFactory) {
        if (log.isDebugEnabled()) {
            boolean traceEnabled = log.isTraceEnabled();
            String logMessage = messageFactory.apply(traceEnabled);
            if (traceEnabled) {
                log.trace(logMessage);
            } else {
                log.debug(logMessage);
            }
        }
    }

    private static final String s0 = "{\"";
    private static final String s1 = "\":";
    private static final String s2 = ",\"";
    private static final String s3 = "\":\"";
    private static final String s4 = "\"";
    private static final String s5 = ",\"traceId\":\"";
    private static final String s6 = ",\"context\":\"";
    private static final String s7 = "}";

    public static String jsonRespBody(int code, @Nullable String msg) {
        return jsonRespBody(code, msg, null, null);
    }

    public static String jsonRespBody(int code, @Nullable String msg, @Nullable String traceId) {
        return jsonRespBody(code, msg, traceId, null);
    }

    public static String jsonRespBody(int code, @Nullable String msg, @Nullable String traceId, @Nullable Object context) {
        StringBuilder b = ThreadContext.getStringBuilder(ThreadContext.sb0);
        b.append(s0).append(SystemConfig.FIZZ_ERR_RESP_CODE_FIELD).append(s1).append(code);
        if (StringUtils.isNotBlank(msg)) {
            b.append(s2).append(SystemConfig.FIZZ_ERR_RESP_MSG_FIELD).append(s3).append(msg).append(s4);
        }
        if (traceId != null) {
            b.append(s5).append(traceId).append(s4);
        }
        if (context != null) {
            b.append(s6).append(context).append(s4);
        }
        b.append(s7);
        return b.toString();
    }

    public static String toQueryString(MultiValueMap<String, String> queryParams) {
        StringBuilder b = ThreadContext.getStringBuilder(ThreadContext.sb0);
        Set<Map.Entry<String, List<String>>> params = queryParams.entrySet();
        int ps = params.size(), cnt = 0;
        try {
            for (Map.Entry<String, List<String>> param : params) {
                String name = param.getKey();
                List<String> values = param.getValue();
                if (values.isEmpty()) {
                    b.append(name);
                } else {
                    int vs = values.size();
                    for (int i = 0; i < vs; ) {
                        b.append(name);
                        String v = values.get(i);
                        if (v != null) {
                            b.append(Consts.S.EQUAL);
                            if (!Consts.S.EMPTY.equals(v)) {
                                /*if (StringUtils.indexOfAny(v, Consts.S.LEFT_BRACE, Consts.S.FORWARD_SLASH, Consts.S.HASH, Consts.S.EQUAL) > -1) {
                                    b.append(URLEncoder.encode(v, Consts.C.UTF8));
                                } else {
                                    b.append(v);
                                }*/
                                b.append(URLDecoder.decode(v, Consts.C.UTF8));
                            }
                        }
                        if ((++i) != vs) {
                            b.append(Consts.S.AND);
                        }
                    }
                }
                if ((++cnt) != ps) {
                    b.append(Consts.S.AND);
                }
            }
            return b.toString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, List<String>> toQueryStringTemplate(MultiValueMap<String, String> queryParams) {
        StringBuilder b = ThreadContext.getStringBuilder(ThreadContext.sb0);
        Set<Map.Entry<String, List<String>>> params = queryParams.entrySet();
        int ps = params.size(), cnt = 0;
        List<String> paramValues = new ArrayList<>();
        for (Map.Entry<String, List<String>> param : params) {
            String name = param.getKey();
            List<String> values = param.getValue();
            if (values.isEmpty()) {
                b.append(name);
            } else {
                int vs = values.size();
                for (int i = 0; i < vs; ) {
                    b.append(name);
                    String v = values.get(i);
                    if (v != null) {
                        b.append(Consts.S.EQUAL);
                        if (!Consts.S.EMPTY.equals(v)) {
                            paramValues.add(v);
                            b.append(Consts.S.LEFT_BRACE).append(paramValues.size()).append(Consts.S.RIGHT_BRACE);
                        }
                    }
                    if ((++i) != vs) {
                        b.append(Consts.S.AND);
                    }
                }
            }
            if ((++cnt) != ps) {
                b.append(Consts.S.AND);
            }
        }
        return Collections.singletonMap(b.toString(), paramValues);
    }

    // the method below will be deprecated.

    @Deprecated
    public static Mono<Void> getDirectResponse(ServerWebExchange exchange) {
        return exchange.getAttribute(WebUtils.directResponse);
    }

    @Deprecated
    public static Map<String, FilterResult> getFilterContext(ServerWebExchange exchange) {
        return exchange.getAttribute(FILTER_CONTEXT);
    }

    @Deprecated
    public static FilterResult getFilterResult(ServerWebExchange exchange, String filter) {
        return getFilterContext(exchange).get(filter);
    }

    @Deprecated
    public static Map<String, Object> getFilterResultData(ServerWebExchange exchange, String filter) {
        return getFilterResult(exchange, filter).data;
    }

    @Deprecated
    public static Object getFilterResultDataItem(ServerWebExchange exchange, String filter, String key) {
        return getFilterResultData(exchange, filter).get(key);
    }

    /**
     * can replace with response(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String body) method.
     * @deprecated
     */
    @Deprecated
    public static Mono<Void> buildDirectResponse(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String body) {
        return buildDirectResponse(exchange.getResponse(), status, headers, body);
    }

    /**
     * can replace with response(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, String body) method.
     * @deprecated
     */
    @Deprecated
    public static Mono<Void> buildDirectResponse(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, String body) {
        if (clientResp.isCommitted()) {
            String msg = "try to response: " + body + ", but server http response is committed and it's status: " + clientResp.getStatusCode();
            log.warn(msg);
            return Mono.error(Utils.runtimeExceptionWithoutStack(msg));
        }
        if (status != null) {
            clientResp.setStatusCode(status);
        }
        if (headers != null) {
            headers.forEach(  (h, vs) -> {clientResp.getHeaders().addAll(h, vs);}  );
        }
        if (body == null) {
            body = Consts.S.EMPTY;
        }
        return clientResp.writeWith(Mono.just(clientResp.bufferFactory().wrap(body.getBytes())));
    }

    /**
     * can replace with responseJson(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String json) method.
     * @deprecated
     */
    @Deprecated
    public static Mono buildJsonDirectResponse(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String json) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return buildDirectResponse(exchange, status, headers, json);
    }

    @Deprecated
    public static Mono buildDirectResponseAndBindContext(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String bodyContent) {
        Mono<Void> mv = buildDirectResponse(exchange, status, headers, bodyContent);
        exchange.getAttributes().put(WebUtils.directResponse, mv);
        return mv;
    }

    @Deprecated
    public static Mono buildJsonDirectResponseAndBindContext(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String json) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return buildDirectResponseAndBindContext(exchange, status, headers, json);
    }

    @Deprecated
    public static void transmitSuccessFilterResult(ServerWebExchange exchange, String filter, Map<String, Object> data) {
        FilterResult fr = FilterResult.SUCCESS_WITH(filter, data);
        bind(exchange, filter, fr);
    }

    @Deprecated
    public static Mono transmitSuccessFilterResultAndEmptyMono(ServerWebExchange exchange, String filter, Map<String, Object> data) {
        transmitSuccessFilterResult(exchange, filter, data);
        return Mono.empty();
    }

    @Deprecated
    public static void transmitFailFilterResult(ServerWebExchange exchange, String filter) {
        FilterResult fr = FilterResult.FAIL(filter);
        bind(exchange, filter, fr);
    }

    @Deprecated
    public static void transmitFailFilterResult(ServerWebExchange exchange, String filter, Throwable cause) {
        FilterResult fr = FilterResult.FAIL_WITH(filter, cause);
        bind(exchange, filter, fr);
    }

    @Deprecated
    public static FilterResult getPrevFilterResult(ServerWebExchange exchange) {
        return getFilterContext(exchange).get(PREV_FILTER_RESULT);
    }

    @Deprecated
    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, int code, String msg) {
        return responseError(exchange, filter, code, msg, null, true);
    }

    @Deprecated
    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, int code, String msg, Throwable t) {
        return responseError(exchange, filter, code, msg, t, true);
    }

    @Deprecated
    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        String traceId = getTraceId(exchange);
        StringBuilder b = ThreadContext.getStringBuilder();
        request2stringBuilder(exchange, b);
        b.append(Consts.S.LINE_SEPARATOR);
        b.append(filter).append(Consts.S.SPACE).append(httpStatus);
        // log.error(b.toString(), LogService.BIZ_ID, traceId);
        org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
        log.error(b.toString());
        transmitFailFilterResult(exchange, filter);
        return buildDirectResponseAndBindContext(exchange, httpStatus, new HttpHeaders(), Consts.S.EMPTY);
    }

    @Deprecated
    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, HttpStatus httpStatus,
                                                         HttpHeaders headers, String content) {
        ServerHttpResponse response = exchange.getResponse();
        String traceId = getTraceId(exchange);
        StringBuilder b = ThreadContext.getStringBuilder();
        request2stringBuilder(exchange, b);
        b.append(Consts.S.LINE_SEPARATOR);
        b.append(filter).append(Consts.S.SPACE).append(httpStatus);
        // log.error(b.toString(), LogService.BIZ_ID, traceId);
        org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
        log.error(b.toString());
        transmitFailFilterResult(exchange, filter);
        headers = headers == null ? new HttpHeaders() : headers;
        content = StringUtils.isBlank(content) ? Consts.S.EMPTY : content;
        return buildDirectResponseAndBindContext(exchange, httpStatus, headers, content);
    }
}
