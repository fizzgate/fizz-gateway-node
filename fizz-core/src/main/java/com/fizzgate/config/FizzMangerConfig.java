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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import com.fizzgate.util.Consts;
import com.fizzgate.util.Result;

import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@Configuration
public class FizzMangerConfig {

    private static final Logger log = LoggerFactory.getLogger(FizzMangerConfig.class);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    public String managerUrl;

    public String pairPath      = "/fizz-manager/dedicated-line/pair";

    public String docPathPrefix = "/fizz-manager/open-doc/open-doc-show/pair";

    @PostConstruct
    public void init() throws Throwable {
        Result<?> result = updateMangerUrl();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    public Result<?> updateMangerUrl() {
        Result<?> result = Result.succ();
        rt.opsForValue().get("fizz_manager_url").defaultIfEmpty(Consts.S.EMPTY)
                        .flatMap(
                                url -> {
                                    if (url.equals(Consts.S.EMPTY)) {
                                        log.warn("no fizz manager url config");
                                    } else {
                                        managerUrl = url;
                                        log.info("fizz manager url: {}", managerUrl);
                                    }
                                    return Mono.empty();
                                }
                        )
                        .onErrorReturn(
                                throwable -> {
                                    result.code = Result.FAIL;
                                    result.msg  = "update fizz manager url error";
                                    result.t    = throwable;
                                    log.error(result.msg, result.t);
                                    return true;
                                },
                                result
                        )
                        .block();
        return result;
    }
}
