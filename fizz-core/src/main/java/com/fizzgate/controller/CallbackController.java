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

package com.fizzgate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.fizzgate.proxy.CallbackReplayReq;
import com.fizzgate.proxy.CallbackService;
import com.fizzgate.util.Consts;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.Result;
import com.fizzgate.util.ThreadContext;

import reactor.core.publisher.Mono;

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
            // log.debug(JacksonUtils.writeValueAsString(req), LogService.BIZ_ID, req.id);
            org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, req.id);
            log.debug(JacksonUtils.writeValueAsString(req));
        }

        return
        callbackService.replay(req)
                .onErrorResume(
                        t -> {
                            return Mono.just(Result.fail(t));
                        }
                )
                .map(
                        r -> {
                            StringBuilder b = ThreadContext.getStringBuilder();
                            b.append(req.id).append(' ').append(req.service).append(' ').append(req.path).append(' ');
                            ServerHttpResponse resp = exchange.getResponse();
                            org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, req.id);
                            if (r.code == Result.SUCC) {
                                // log.info(b.append("replay success").toString(), LogService.BIZ_ID, req.id);
                                log.info(b.append("replay success").toString());
                                resp.setStatusCode(HttpStatus.OK);
                                return Consts.S.EMPTY;
                            } else {
                                b.append("replay error:\n").append(r);
                                // log.error(b.toString(), LogService.BIZ_ID, req.id);
                                log.error(b.toString());
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
