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

package com.fizzgate.plugin.auth;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fizzgate.config.AggregateRedisConfig;
import com.fizzgate.util.Consts;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ReactorUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author hongqiaowei
 */

@Service
public class AppService {

    private static final Logger log            = LoggerFactory.getLogger(AppService.class);

    private static final String fizzApp        = "fizz_app";

    private static final String fizzAppChannel = "fizz_app_channel";

    private Map<String, App>  appMap    = new HashMap<>(32);

    private Map<Integer, App> oldAppMap = new HashMap<>(32);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        this.init(this::lsnAppChange);
    }

    public void refreshLocalCache() throws Throwable {
        this.init(null);
    }

    private void init(Supplier<Mono<Throwable>> doAfterLoadCache) throws Throwable {
        Map<String, App> appMapTmp = new HashMap<>(32);
        Map<Integer, App> oldAppMapTmp = new HashMap<>(32);
        final Throwable[] throwable = new Throwable[1];
        Throwable error = Mono.just(Objects.requireNonNull(rt.opsForHash().entries(fizzApp)
                .defaultIfEmpty(new AbstractMap.SimpleEntry<>(ReactorUtils.OBJ, ReactorUtils.OBJ)).onErrorStop().doOnError(t -> {
                    log.info(null, t);
                })
                .concatMap(e -> {
                    Object k = e.getKey();
                    if (k == ReactorUtils.OBJ) {
                        return Flux.just(e);
                    }
                    String json = (String) e.getValue();
                    // log.info("init app: {}", json, LogService.BIZ_ID, k.toString());
                    ThreadContext.put(Consts.TRACE_ID, k.toString());
                    log.info("init app: {}", json);
                    try {
                        App app = JacksonUtils.readValue(json, App.class);
                        oldAppMapTmp.put(app.id, app);
                        updateAppMap(app, appMapTmp);
                        return Flux.just(e);
                    } catch (Throwable t) {
                        // throwable[0] = t;
                        log.warn(json, t);
                        // return Flux.error(t);
                        return Flux.just(e);
                    }
                }).blockLast())).flatMap(
                e -> {
                    if (throwable[0] != null) {
                        return Mono.error(throwable[0]);
                    }
                    if (doAfterLoadCache != null) {
                        return doAfterLoadCache.get();
                    } else {
                        return Mono.just(ReactorUtils.EMPTY_THROWABLE);
                    }
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            // throw error;
            log.error(Consts.S.EMPTY, error);
        }

        appMap = appMapTmp;
        oldAppMap = oldAppMapTmp;
    }

    private Mono<Throwable> lsnAppChange() {
        final Throwable[] throwable = new Throwable[1];
        final boolean[] b = {false};
        rt.listenToChannel(fizzAppChannel).doOnError(t -> {
            throwable[0] = t;
            b[0] = false;
            log.error("lsn " + fizzAppChannel, t);
        }).doOnSubscribe(
                s -> {
                    b[0] = true;
                    log.info("success to lsn on " + fizzAppChannel);
                }
        ).doOnNext(msg -> {
            String json = msg.getMessage();
            // log.info("app change: " + json, LogService.BIZ_ID, "ac" + System.currentTimeMillis());
            ThreadContext.put(Consts.TRACE_ID, "ac" + System.currentTimeMillis());
            log.info("app change: " + json);
            try {
                App app = JacksonUtils.readValue(json, App.class);
                App r = oldAppMap.remove(app.id);
                if (!app.isDeleted && r != null) {
                    appMap.remove(r.app);
                }
                updateAppMap(app, appMap);
                if (!app.isDeleted) {
                    oldAppMap.put(app.id, app);
                }
            } catch (Throwable t) {
                log.info(json, t);
            }
        }).subscribe();
        Throwable t = throwable[0];
        while (!b[0]) {
            if (t != null) {
                return Mono.error(t);
            } else {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    return Mono.error(e);
                }
            }
        }
        return Mono.just(ReactorUtils.EMPTY_THROWABLE);
    }

    private void updateAppMap(App app, Map<String, App> appMap) {
        if (app.isDeleted) {
            App removedApp = appMap.remove(app.app);
            log.info("remove " + removedApp);
        } else {
            App existApp = appMap.get(app.app);
            appMap.put(app.app, app);
            if (existApp == null) {
                log.info("add " + app);
            } else {
                log.info("update " + existApp + " with " + app);
            }
        }
    }

    public App getApp(String app) {
        return appMap.get(app);
    }

    public Map<String, App> getAppMap() {
        return appMap;
    }
}
