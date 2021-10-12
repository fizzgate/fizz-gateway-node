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

package we.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.proxy.CallbackReplayReq;
import we.proxy.CallbackService;
import we.util.Consts;
import we.util.JacksonUtils;
import we.util.ReactiveResult;
import we.util.ThreadContext;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@RestController
@RequestMapping(value = "/admin")
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    @Resource
    private CallbackService callbackService;

    @PostMapping("/callback")
    public Mono<String> callback(ServerWebExchange exchange, @RequestBody CallbackReplayReq req) {

        if (log.isDebugEnabled()) {
            log.debug(JacksonUtils.writeValueAsString(req), LogService.BIZ_ID, req.id);
        }

        return
        callbackService.replay(req)
                .onErrorResume(
                        t -> {
                            return Mono.just(ReactiveResult.fail(t));
                        }
                )
                .map(
                        r -> {
                            StringBuilder b = ThreadContext.getStringBuilder();
                            b.append(req.id).append(' ').append(req.service).append(' ').append(req.path).append(' ');
                            ServerHttpResponse resp = exchange.getResponse();
                            if (r.code == ReactiveResult.SUCC) {
                                log.info(b.append("replay success").toString(), LogService.BIZ_ID, req.id);
                                resp.setStatusCode(HttpStatus.OK);
                                return Consts.S.EMPTY;
                            } else {
                                b.append("replay error:\n");
                                r.toStringBuilder(b);
                                log.error(b.toString(), LogService.BIZ_ID, req.id);
                                resp.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                                if (r.msg != null) {
                                    return r.msg;
                                }
                                if (r.t != null) {
                                    return r.t.getMessage();
                                }
                                return "unknown error";
                            }
                        }
                )
                ;
    }

}
