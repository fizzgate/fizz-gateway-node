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
import org.springframework.beans.factory.annotation.Value;
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
import we.plugin.auth.App;
import we.plugin.auth.AppService;
import we.util.DateTimeUtils;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author hongqiaowei
 */

@RestController
@RequestMapping(SystemConfig.DEFAULT_GATEWAY_PREFIX + "/_fizz-pairing")
public class FizzApiPairingController {

    private static final Logger log = LoggerFactory.getLogger(FizzApiPairingController.class);

    @Resource
    private SystemConfig systemConfig;

    @Resource
    private AppService   appService;

    @Value("${fizz.api.pairing.request.timeliness:300}")
    private int timeliness = 300; // unit: sec

    @GetMapping("/pair")
    public Mono<Void> pair(ServerWebExchange exchange) {

        ServerHttpRequest   request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
        HttpHeaders headers = request.getHeaders();

        String appId = WebUtils.getAppId(exchange);
        if (appId == null) {
            return WebUtils.buildDirectResponse(response, null, null, "请求无应用信息");
        }
        App app = appService.getApp(appId);
        if (app == null) {
            return WebUtils.buildDirectResponse(response, null, null, "系统无" + appId + "应用信息");
        }

        String timestamp = getTimestamp(headers);
        if (timestamp == null) {
            return WebUtils.buildDirectResponse(response, null, null, "请求无时间戳");
        }
        try {
            long ts = Long.parseLong(timestamp);
            LocalDateTime now = LocalDateTime.now();
            long start = DateTimeUtils.toMillis(now.minusSeconds(timeliness));
            long end   = DateTimeUtils.toMillis(now.plusSeconds (timeliness));
            if (start <= ts && ts <= end) {
                // valid
            } else {
                return WebUtils.buildDirectResponse(response, null, null, "请求时间戳无效");
            }
        } catch (NumberFormatException e) {
            return WebUtils.buildDirectResponse(response, null, null, "请求时间戳无效");
        }

        String sign = getSign(headers);
        if (sign == null) {
            return WebUtils.buildDirectResponse(response, null, null, "请求未签名");
        }

        boolean equals = PairingUtils.checkSign(appId, timestamp, app.secretkey, sign);

        if (equals) {
			// TODO: 响应文档集
            return Mono.empty();
        } else {
            log.warn("request authority: app {}, timestamp {}, sign {} invalid", appId, timestamp, sign);
            return WebUtils.buildDirectResponse(response, null, null, "请求签名无效");
        }
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
