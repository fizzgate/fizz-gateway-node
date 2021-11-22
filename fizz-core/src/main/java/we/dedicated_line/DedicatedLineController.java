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

package we.dedicated_line;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.config.FizzMangerConfig;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.proxy.FizzWebClient;
import we.util.DateTimeUtils;
import we.util.Result;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = SystemConfig.FIZZ_DEDICATED_LINE_SERVER_ENABLE, havingValue = "true")
@RestController
@RequestMapping(SystemConfig.DEFAULT_GATEWAY_PREFIX + "/_fizz-dedicated-line")
public class DedicatedLineController {

    private static final Logger log = LoggerFactory.getLogger(DedicatedLineController.class);

    @Resource
    private SystemConfig            systemConfig;

    @Resource
    private DedicatedLineService    dedicatedLineService;

    @Resource
    private FizzMangerConfig        fizzMangerConfig;

    @Resource
    private FizzWebClient           fizzWebClient;

    private Result<?> auth(ServerWebExchange exchange) {
        String dedicatedLineId = WebUtils.getDedicatedLineId(exchange);
        if (dedicatedLineId == null) {
            return Result.fail("no dedicated line id in request");
        }

        String timestamp = WebUtils.getDedicatedLineTimestamp(exchange);
        if (timestamp == null) {
            return Result.fail("no timestamp in request");
        }
        try {
            long ts = Long.parseLong(timestamp);
            LocalDateTime now = LocalDateTime.now();
            long timeliness = systemConfig.fizzDedicatedLineClientRequestTimeliness();
            long start = DateTimeUtils.toMillis(now.minusSeconds(timeliness));
            long end   = DateTimeUtils.toMillis(now.plusSeconds (timeliness));
            if (start <= ts && ts <= end) {
                // valid
            } else {
                return Result.fail("request timestamp invalid");
            }
        } catch (NumberFormatException e) {
            return Result.fail("request timestamp format invalid");
        }

        String sign = WebUtils.getDedicatedLineSign(exchange);
        if (sign == null) {
            return Result.fail("no sign in request");
        }

        String pairCodeSecretKey = dedicatedLineService.getSignSecretKey(dedicatedLineId);
        boolean equals = DedicatedLineUtils.checkSign(dedicatedLineId, timestamp, pairCodeSecretKey, sign);
        if (!equals) {
            String traceId = WebUtils.getTraceId(exchange);
            log.warn("{} request authority: dedicated line id {}, timestamp {}, sign {} invalid", traceId, dedicatedLineId, timestamp, sign, LogService.BIZ_ID, traceId);
            return Result.fail("request sign invalid");
        }
        return Result.succ();
    }

    @GetMapping("/pair")
    public Mono<Void> pair(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String managerUrl = fizzMangerConfig.managerUrl;
        if (managerUrl == null) {
            return WebUtils.response(response, HttpStatus.INTERNAL_SERVER_ERROR, null, "no fizz manager url config");
        }
        String address = managerUrl + fizzMangerConfig.pairPath;

        String traceId = WebUtils.getTraceId(exchange);
        Mono<ClientResponse> remoteResponseMono = fizzWebClient.send(traceId, request.getMethod(), address, request.getHeaders(), null);
        return
                remoteResponseMono.flatMap(
                        remoteResp -> {
                            response.setStatusCode(remoteResp.statusCode());
                            HttpHeaders respHeaders = response.getHeaders();
                            HttpHeaders remoteRespHeaders = remoteResp.headers().asHttpHeaders();
                            respHeaders.putAll(remoteRespHeaders);
                            if (log.isDebugEnabled()) {
                                StringBuilder sb = ThreadContext.getStringBuilder();
                                WebUtils.response2stringBuilder(traceId, remoteResp, sb);
                                log.debug(sb.toString(), LogService.BIZ_ID, traceId);
                            }
                            return response.writeWith (  remoteResp.body(BodyExtractors.toDataBuffers()) )
                                           .doOnError (   throwable -> cleanup(remoteResp)               )
                                           .doOnCancel(          () -> cleanup(remoteResp)               );
                        }
                );
    }

    @GetMapping("/doc/**")
    public Mono<Void> doc(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        Result<?> auth = auth(exchange);
        if (auth.code == Result.SUCC) {
            String managerUrl = fizzMangerConfig.managerUrl;
            if (managerUrl == null) {
                return WebUtils.response(response, HttpStatus.INTERNAL_SERVER_ERROR, null, "no fizz manager url config");
            }
            ServerHttpRequest request = exchange.getRequest();
            String uri = request.getURI().toString();
            int dp = uri.indexOf("doc");
            String address = managerUrl + fizzMangerConfig.docPathPrefix + uri.substring(dp + 3);

            String traceId = WebUtils.getTraceId(exchange);
            Mono<ClientResponse> remoteResponseMono = fizzWebClient.send(traceId, request.getMethod(), address, request.getHeaders(), null);
            return
                   remoteResponseMono.flatMap(
                                             remoteResp -> {
                                                 response.setStatusCode(remoteResp.statusCode());
                                                 HttpHeaders respHeaders = response.getHeaders();
                                                 HttpHeaders remoteRespHeaders = remoteResp.headers().asHttpHeaders();
                                                 respHeaders.putAll(remoteRespHeaders);
                                                 if (log.isDebugEnabled()) {
                                                     StringBuilder sb = ThreadContext.getStringBuilder();
                                                     WebUtils.response2stringBuilder(traceId, remoteResp, sb);
                                                     log.debug(sb.toString(), LogService.BIZ_ID, traceId);
                                                 }
                                                 return response.writeWith (  remoteResp.body(BodyExtractors.toDataBuffers()) )
                                                                .doOnError (   throwable -> cleanup(remoteResp)               )
                                                                .doOnCancel(          () -> cleanup(remoteResp)               );
                                             }
                                     );

        } else {
            return WebUtils.response(response, HttpStatus.FORBIDDEN, null, auth.msg);
        }
    }

    private void cleanup(ClientResponse clientResponse) {
        if (clientResponse != null) {
            clientResponse.bodyToMono(Void.class).subscribe();
        }
    }
}
