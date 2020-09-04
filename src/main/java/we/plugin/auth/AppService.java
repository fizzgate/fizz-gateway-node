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
package we.plugin.auth;

import we.flume.clients.log4j2appender.LogService;
import we.listener.AggregateRedisConfig;
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.ReactorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author lancer
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
                    Object v = e.getValue();
                    log.info(k.toString() + Constants.Symbol.COLON + v.toString(), LogService.BIZ_ID, k.toString());
                    String json = (String) v;
                    try {
                        App app = JacksonUtils.readValue(json, App.class);
                        oldAppMap.put(app.id, app);
                        updateAppMap(app);
                        return Flux.just(e);
                    } catch (Throwable t) {
                        throwable[0] = t;
                        log.info(json, t);
                        return Flux.error(t);
                    }
                }).blockLast())).flatMap(
                e -> {
                    if (throwable[0] != null) {
                        return Mono.error(throwable[0]);
                    }
                    return lsnAppChange();
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            throw error;
        }
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
            log.info(json, LogService.BIZ_ID, "ac" + System.currentTimeMillis());
            try {
                App app = JacksonUtils.readValue(json, App.class);
                App r = oldAppMap.remove(app.id);
                if (app.isDeleted != App.DELETED && r != null) {
                    appMap.remove(r.app);
                }
                updateAppMap(app);
                if (app.isDeleted != App.DELETED) {
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

    private void updateAppMap(App app) {
        if (app.isDeleted == App.DELETED) {
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
