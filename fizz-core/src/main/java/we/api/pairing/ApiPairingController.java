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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.auth.App;
import we.plugin.auth.AppService;
import we.util.DateTimeUtils;
import we.util.JacksonUtils;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hongqiaowei
 */

@ConditionalOnBean({ApiPairingDocSetService.class})
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

    @GetMapping("/pair")
    public Mono<Void> pair(ServerWebExchange exchange) {

        ServerHttpRequest   request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
        HttpHeaders headers = request.getHeaders();

        String appId = WebUtils.getAppId(exchange);
        if (appId == null) {
            return WebUtils.buildDirectResponse(response, null, null, "no app info in request");
        }
        App app = appService.getApp(appId);
        if (app == null) {
            return WebUtils.buildDirectResponse(response, null, null, appId + " not exists");
        }

        String timestamp = getTimestamp(headers);
        if (timestamp == null) {
            return WebUtils.buildDirectResponse(response, null, null, "no timestamp in request");
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
                return WebUtils.buildDirectResponse(response, null, null, "request timestamp invalid");
            }
        } catch (NumberFormatException e) {
            return WebUtils.buildDirectResponse(response, null, null, "request timestamp invalid");
        }

        String sign = getSign(headers);
        if (sign == null) {
            return WebUtils.buildDirectResponse(response, null, null, "no sign in request");
        }

        boolean equals = ApiPairingUtils.checkSign(appId, timestamp, app.secretkey, sign);
        if (equals) {
            List<AppApiPairingDocSet> docs = getAppDocSet(appId);
            String docsJson = JacksonUtils.writeValueAsString(docs);
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(docsJson.getBytes())));
        } else {
            log.warn("{}request authority: app {}, timestamp {}, sign {} invalid",
                  exchange.getLogPrefix(), appId,  timestamp,    sign, LogService.BIZ_ID, WebUtils.getTraceId(exchange));
            return WebUtils.buildDirectResponse(response, null, null, "request sign invalid");
        }
    }

    private List<AppApiPairingDocSet> getAppDocSet(String appId) {
        Map<Integer, ApiPairingDocSet> docSetMap = apiPairingDocSetService.getDocSetMap();
        ArrayList<AppApiPairingDocSet> result = ThreadContext.getArrayList();
        for (Map.Entry<Integer, ApiPairingDocSet> entry : docSetMap.entrySet()) {
            ApiPairingDocSet ds = entry.getValue();
            AppApiPairingDocSet appDocSet = new AppApiPairingDocSet();
            appDocSet.id = ds.id;
            appDocSet.name = ds.name;
            appDocSet.services = ds.docs.stream().map(d -> d.service).collect(Collectors.toSet());
            appDocSet.enabled = false;
            if (ds.appIds.contains(appId)) {
                appDocSet.enabled = true;
            }
            result.add(appDocSet);
        }
        return result;
    }

    private String getSign(HttpHeaders headers) {
        List<String> signHdrs = systemConfig.getSignHeaders();
        for (int i = 0; i < signHdrs.size(); i++) {
            String v = headers.getFirst(signHdrs.get(i));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String getTimestamp(HttpHeaders headers) {
        List<String> tsHdrs = systemConfig.getTimestampHeaders();
        for (int i = 0; i < tsHdrs.size(); i++) {
            String v = headers.getFirst(tsHdrs.get(i));
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
