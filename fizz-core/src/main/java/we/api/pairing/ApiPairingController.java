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

package we.api.pairing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import we.plugin.auth.App;
import we.plugin.auth.AppService;
import we.proxy.FizzWebClient;
import we.util.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = SystemConfig.FIZZ_API_PAIRING_SERVER_ENABLE, havingValue = "true")
@RestController
@RequestMapping(SystemConfig.DEFAULT_GATEWAY_PREFIX + "/_fizz-pairing")
public class ApiPairingController {

    private static final Logger log = LoggerFactory.getLogger(ApiPairingController.class);

    @Resource
    private SystemConfig            systemConfig;

    @Resource
    private AppService              appService;

    @Resource
    private ApiPairingDocSetService apiPairingDocSetService;

    @Resource
    private FizzMangerConfig        fizzMangerConfig;

    @Resource
    private FizzWebClient           fizzWebClient;

    private Result<?> auth(ServerWebExchange exchange) {
        String appId = WebUtils.getAppId(exchange);
        if (appId == null) {
            return Result.fail("no app info in request");
        }
        App app = appService.getApp(appId);
        if (app == null) {
            return Result.fail(appId + " not exists");
        }

        String timestamp = WebUtils.getTimestamp(exchange);
        if (timestamp == null) {
            return Result.fail("no timestamp in request");
        }
        try {
            long ts = Long.parseLong(timestamp);
            LocalDateTime now = LocalDateTime.now();
            long timeliness = systemConfig.fizzApiPairingRequestTimeliness();
            long start = DateTimeUtils.toMillis(now.minusSeconds(timeliness));
            long end   = DateTimeUtils.toMillis(now.plusSeconds (timeliness));
            if (start <= ts && ts <= end) {
                // valid
            } else {
                return Result.fail("request timestamp invalid");
            }
        } catch (NumberFormatException e) {
            return Result.fail("request timestamp invalid");
        }

        String sign = WebUtils.getSign(exchange);
        if (sign == null) {
            return Result.fail("no sign in request");
        }
        boolean equals = ApiPairingUtils.checkSign(appId, timestamp, app.secretkey, sign);
        if (!equals) {
            log.warn("{}request authority: app {}, timestamp {}, sign {} invalid", exchange.getLogPrefix(), appId, timestamp, sign, LogService.BIZ_ID, WebUtils.getTraceId(exchange));
            return Result.fail("request sign invalid");
        }
        return Result.succ();
    }

    @GetMapping("/pair")
    public Mono<Void> pair(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        Result<?> auth = auth(exchange);
        if (auth.code == Result.SUCC) {
            String appId = WebUtils.getAppId(exchange);
            List<AppApiPairingDocSet> docs = getAppDocSet(appId);
            String docsJson = JacksonUtils.writeValueAsString(docs);
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(docsJson.getBytes())));
        } else {
            return WebUtils.buildDirectResponse(response, HttpStatus.FORBIDDEN, null, auth.msg);
        }
    }

    private List<AppApiPairingDocSet> getAppDocSet(String appId) {
        Map<Long, ApiPairingDocSet> docSetMap = apiPairingDocSetService.getDocSetMap();
        ArrayList<AppApiPairingDocSet> result = ThreadContext.getArrayList();
        for (Map.Entry<Long, ApiPairingDocSet> entry : docSetMap.entrySet()) {
            ApiPairingDocSet ds = entry.getValue();
            AppApiPairingDocSet appDocSet = new AppApiPairingDocSet();
            appDocSet.id          = ds.id;
            appDocSet.name        = ds.name;
            appDocSet.description = ds.description;
            appDocSet.services    = ds.docs.stream().map(d -> d.service).collect(Collectors.toSet());
            appDocSet.enabled     = false;
            if (ds.appIds.contains(appId)) {
                appDocSet.enabled = true;
            }
            result.add(appDocSet);
        }
        return result;
    }

    @GetMapping("/doc/**")
    public Mono<Void> doc(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        Result<?> auth = auth(exchange);
        if (auth.code == Result.SUCC) {
            String managerUrl = fizzMangerConfig.managerUrl;
            if (managerUrl == null) {
                return WebUtils.buildDirectResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, null, "no fizz manager url config");
            }
            ServerHttpRequest request = exchange.getRequest();
            String uri = request.getURI().toString();
            int dp = uri.indexOf("doc");
            String address = managerUrl + fizzMangerConfig.docPathPrefix + uri.substring(dp + 3);

            String traceId = WebUtils.getTraceId(exchange);
            Mono<ClientResponse> remoteResponseMono = fizzWebClient.send(traceId, request.getMethod(), address, request.getHeaders(), request.getBody());
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
            return WebUtils.buildDirectResponse(response, HttpStatus.FORBIDDEN, null, auth.msg);
        }
    }

    private void cleanup(ClientResponse clientResponse) {
        if (clientResponse != null) {
            clientResponse.bodyToMono(Void.class).subscribe();
        }
    }
}
