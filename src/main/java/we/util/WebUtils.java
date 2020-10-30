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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.filter.FilterResult;
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;

import java.util.*;

/**
 * @author hongqiaowei
 */

public abstract class WebUtils {

    private static final Logger       log                = LoggerFactory.getLogger(WebUtils.class);

    public  static final String       APP_HEADER         = "fizz-appid";

    private static final String       directResponse     = "directResponse";

    public  static final String       FILTER_CONTEXT     = "filterContext";

    public  static final String       APPEND_HEADERS     = "appendHeaders";

    public  static final String       PREV_FILTER_RESULT = "prevFilterResult";

    public  static final String       request_path       = "reqPath";

    private static final String       SERVICE_ID         = "serviceId";

    private static final String       xForwardedFor      = "X-FORWARDED-FOR";

    private static final String       unknown            = "unknown";

    private static final String       loopBack           = "127.0.0.1";

    private static final String       binaryAddress      = "0:0:0:0:0:0:0:1";

    public  static       boolean      logResponseBody    = false;

    public  static       Set<String>  logHeaderSet       = Collections.EMPTY_SET;

    private static final String       response           = " response ";

    private static final String       originIp           = "originIp";

    public static String getHeaderValue(ServerWebExchange exchange, String header) {
        return exchange.getRequest().getHeaders().getFirst(header);
    }

    public static List<String> getHeaderValues(ServerWebExchange exchange, String header) {
        return exchange.getRequest().getHeaders().get(header);
    }

    public static String getAppId(ServerWebExchange exchange) {
        return exchange.getAttribute(APP_HEADER);
    }

    public static String getServiceId(ServerWebExchange exchange) {
        String svc = exchange.getAttribute(SERVICE_ID);
        if (svc == null) {
            String p = exchange.getRequest().getPath().value();
            int pl = p.length();
            if (pl < 15) {
            } else {
                boolean b = false;
                if (p.charAt(2) == 'r' && p.charAt(3) == 'o' && p.charAt(4) == 'x') {
                    b = true;
                }
                if (b) {
                    byte i = 9;
                    if (p.charAt(6) == 't') {
                        i = 13;
                    }
                    for (; i < pl; i++) {
                        if (p.charAt(i) == Constants.Symbol.FORWARD_SLASH) {
                            if (p.charAt(6) == 't') {
                                svc = p.substring(11, i);
                            } else {
                                svc = p.substring(7, i);
                            }
                            break;
                        }
                    }
                    exchange.getAttributes().put(SERVICE_ID, svc);
                }
            }
        }
        return svc;
    }

    public static Mono<Void> getDirectResponse(ServerWebExchange exchange) {
        return (Mono<Void>) exchange.getAttributes().get(WebUtils.directResponse);
    }

    public static Map<String, FilterResult> getFilterContext(ServerWebExchange exchange) {
        return (Map<String, FilterResult>) exchange.getAttributes().get(FILTER_CONTEXT);
    }

    public static FilterResult getFilterResult(ServerWebExchange exchange, String filter) {
        return getFilterContext(exchange).get(filter);
    }

    public static Map<String, Object> getFilterResultData(ServerWebExchange exchange, String filter) {
        return getFilterResult(exchange, filter).data;
    }

    public static Object getFilterResultDataItem(ServerWebExchange exchange, String filter, String key) {
        return getFilterResultData(exchange, filter).get(key);
    }

    public static Mono<Void> buildDirectResponse(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String bodyContent) {
        return buildDirectResponse(exchange.getResponse(), status, headers, bodyContent);
    }

    public static Mono buildDirectResponseAndBindContext(ServerWebExchange exchange, HttpStatus status, HttpHeaders headers, String bodyContent) {
        Mono<Void> mv = buildDirectResponse(exchange, status, headers, bodyContent);
        exchange.getAttributes().put(WebUtils.directResponse, mv);
        return mv;
    }

    public static Mono buildJsonDirectResponse(ServerWebExchange exchange, HttpStatus status, @Nullable HttpHeaders headers, String json) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return buildDirectResponse(exchange, status, headers, json);
    }

    public static Mono buildJsonDirectResponseAndBindContext(ServerWebExchange exchange, HttpStatus status, @Nullable HttpHeaders headers, String json) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return buildDirectResponseAndBindContext(exchange, status, headers, json);
    }

    public static Mono<Void> buildDirectResponse(ServerHttpResponse clientResp, HttpStatus status, HttpHeaders headers, String bodyContent) {
        if (clientResp.isCommitted()) {
            log.warn(bodyContent + ", but client resp is committed, " + clientResp.getStatusCode());
            return Mono.error(new RuntimeException(bodyContent, null, false, false) {});
        }
        clientResp.setStatusCode(status);
        headers.forEach(
                (h, vs) -> {
                    clientResp.getHeaders().addAll(h, vs);
                }
        );
        return clientResp
                .writeWith(Mono.just(clientResp.bufferFactory().wrap(bodyContent.getBytes())));
    }

    public static void transmitSuccessFilterResult(ServerWebExchange exchange, String filter, Map<String, Object> data) {
        FilterResult fr = FilterResult.SUCCESS_WITH(filter, data);
        bind(exchange, filter, fr);
    }

    public static Mono transmitSuccessFilterResultAndEmptyMono(ServerWebExchange exchange, String filter, Map<String, Object> data) {
        transmitSuccessFilterResult(exchange, filter, data);
        return Mono.empty();
    }

    public static void transmitFailFilterResult(ServerWebExchange exchange, String filter) {
        FilterResult fr = FilterResult.FAIL(filter);
        bind(exchange, filter, fr);
    }

    public static void transmitFailFilterResult(ServerWebExchange exchange, String filter, Throwable cause) {
        FilterResult fr = FilterResult.FAIL_WITH(filter, cause);
        bind(exchange, filter, fr);
    }

    private static void bind(ServerWebExchange exchange, String filter, FilterResult fr) {
        Map<String, FilterResult> fc = getFilterContext(exchange);
        fc.put(filter, fr);
        fc.put(PREV_FILTER_RESULT, fr);
    }

    public static FilterResult getPrevFilterResult(ServerWebExchange exchange) {
        return getFilterContext(exchange).get(PREV_FILTER_RESULT);
    }

    public static String getReqPath(ServerWebExchange exchange) {
        String path = exchange.getAttribute(request_path);
        if (path == null) {
            path = exchange.getRequest().getPath().value();
            path = path.substring(path.indexOf(Constants.Symbol.FORWARD_SLASH, 11), path.length());
            exchange.getAttributes().put(request_path, path);
        }
        return path;
    }

    public static String getRelativeUri(ServerWebExchange exchange) {
        String relativeUri = getReqPath(exchange);
        String qry = exchange.getRequest().getURI().getQuery();
        if (qry != null) {
            if (StringUtils.indexOfAny(qry, Constants.Symbol.LEFT_BRACE, Constants.Symbol.FORWARD_SLASH, Constants.Symbol.HASH) > 0) {
                qry = exchange.getRequest().getURI().getRawQuery();
            }
            relativeUri = relativeUri + Constants.Symbol.QUESTION + qry;
        }
        return relativeUri;
    }

    public static Map<String, String> getAppendHeaders(ServerWebExchange exchange) {
        return (Map<String, String>) exchange.getAttributes().get(APPEND_HEADERS);
    }

    public static Map<String, String> appendHeader(ServerWebExchange exchange, String name, String value) {
        Map<String, String> hdrs = getAppendHeaders(exchange);
        hdrs.put(name, value);
        return hdrs;
    }

    public static void request2stringBuilder(ServerWebExchange exchange, StringBuilder b) {
        ServerHttpRequest req = exchange.getRequest();
        request2stringBuilder(req.getId(), req.getMethod(), req.getURI().toString(), req.getHeaders(), null, b);
    }

    public static void request2stringBuilder(String reqId, HttpMethod method, String uri, HttpHeaders headers, Object body, StringBuilder b) {
        b.append(reqId).append(Constants.Symbol.SPACE).append(method).append(Constants.Symbol.SPACE).append(uri);
        if (headers != null) {
            final boolean[] f = {false};
            logHeaderSet.forEach(
                    h -> {
                        String v = headers.getFirst(h);
                        if (v != null) {
                            if (!f[0]) {
                                b.append(Constants.Symbol.LINE_SEPARATOR);
                                f[0] = true;
                            }
                            Utils.addTo(b, h, Constants.Symbol.EQUAL, v, Constants.Symbol.TWO_SPACE_STR);
                        }
                    }
            );
        }
        // body to b
    }

    public static void response2stringBuilder(String rid, ClientResponse clientResponse, StringBuilder b) {
        b.append(rid).append(response).append(clientResponse.statusCode());
        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
        final boolean[] f = {false};
        logHeaderSet.forEach(
                h -> {
                    String v = headers.getFirst(h);
                    if (v != null) {
                        if (!f[0]) {
                            b.append(Constants.Symbol.LINE_SEPARATOR);
                            f[0] = true;
                        }
                        Utils.addTo(b, h, Constants.Symbol.EQUAL, v, Constants.Symbol.TWO_SPACE_STR);
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
        String rid = exchange.getRequest().getId();
        // Schedulers.parallel().schedule(() -> {
            StringBuilder b = ThreadContext.getStringBuilder();
            request2stringBuilder(exchange, b);
            // if (reqBody[0] != null) {
            //     DataBufferUtils.release(reqBody[0]);
            // }
            b.append(Constants.Symbol.LINE_SEPARATOR);
            b.append(filter).append(Constants.Symbol.SPACE).append(code).append(Constants.Symbol.SPACE).append(msg);
            if (t == null) {
                log.error(b.toString(), LogService.BIZ_ID, rid);
            } else {
                log.error(b.toString(), LogService.BIZ_ID, rid, t);
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
        if (bindContext) {
            return buildJsonDirectResponseAndBindContext(exchange, HttpStatus.OK, null, RespEntity.toJson(code, msg, rid));
        } else {
            return buildJsonDirectResponse(exchange, HttpStatus.OK, null, RespEntity.toJson(code, msg, rid));
        }
    }

    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, int code, String msg) {
        return responseError(exchange, filter, code, msg, null, true);
    }

    public static Mono<Void> responseErrorAndBindContext(ServerWebExchange exchange, String filter, int code, String msg, Throwable t) {
        return responseError(exchange, filter, code, msg, t, true);
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
                ip = StringUtils.split(v, Constants.Symbol.COMMA)[0].trim();
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
}
