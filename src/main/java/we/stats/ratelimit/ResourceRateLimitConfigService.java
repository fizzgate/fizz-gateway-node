/*
 *  Copyright (C) 2021 the original author or authors.
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
package we.stats.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.auth.App;
import we.util.JacksonUtils;
import we.util.ReactorUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author hongqiaowei
 */

@Service
public class ResourceRateLimitConfigService {

    private static final Logger log                  = LoggerFactory.getLogger(ResourceRateLimitConfigService.class);

    private static final String fizzRateLimit        = "fizz_rate_limit";

    private static final String fizzRateLimitChannel = "fizz_rate_limit_channel";

    private Map<String,  ResourceRateLimitConfig> resourceRateLimitConfigMap    = new HashMap<>(32);

    private Map<Integer, ResourceRateLimitConfig> oldResourceRateLimitConfigMap = new HashMap<>(32);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        final Throwable[] throwable = new Throwable[1];
        Throwable error = Mono.just(Objects.requireNonNull(rt.opsForHash().entries(fizzRateLimit)
                .defaultIfEmpty(new AbstractMap.SimpleEntry<>(ReactorUtils.OBJ, ReactorUtils.OBJ)).onErrorStop().doOnError(t -> {
                    log.info(null, t);
                })
                .concatMap(e -> {
                    Object k = e.getKey();
                    if (k == ReactorUtils.OBJ) {
                        return Flux.just(e);
                    }
                    Object v = e.getValue();
                    log.info("rateLimitConfig: " + v.toString(), LogService.BIZ_ID, k.toString());
                    String json = (String) v;
                    try {
                        ResourceRateLimitConfig rrlc = JacksonUtils.readValue(json, ResourceRateLimitConfig.class);
                        oldResourceRateLimitConfigMap.put(rrlc.id, rrlc);
                        updateResourceRateLimitConfigMap(rrlc);
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
                    return lsnResourceRateLimitConfigChange();
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            throw error;
        }
    }

    private Mono<Throwable> lsnResourceRateLimitConfigChange() {
        final Throwable[] throwable = new Throwable[1];
        final boolean[] b = {false};
        rt.listenToChannel(fizzRateLimitChannel).doOnError(t -> {
            throwable[0] = t;
            b[0] = false;
            log.error("lsn " + fizzRateLimitChannel, t);
        }).doOnSubscribe(
                s -> {
                    b[0] = true;
                    log.info("success to lsn on " + fizzRateLimitChannel);
                }
        ).doOnNext(msg -> {
            String json = msg.getMessage();
            log.info("channel recv rate limit config: " + json, LogService.BIZ_ID, "rrlc" + System.currentTimeMillis());
            try {
                ResourceRateLimitConfig rrlc = JacksonUtils.readValue(json, ResourceRateLimitConfig.class);
                ResourceRateLimitConfig r = oldResourceRateLimitConfigMap.remove(rrlc.id);
                if (rrlc.isDeleted != ResourceRateLimitConfig.DELETED && r != null) {
                    resourceRateLimitConfigMap.remove(r.resource);
                }
                updateResourceRateLimitConfigMap(rrlc);
                if (rrlc.isDeleted != ResourceRateLimitConfig.DELETED) {
                    oldResourceRateLimitConfigMap.put(rrlc.id, rrlc);
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

    private void updateResourceRateLimitConfigMap(ResourceRateLimitConfig rrlc) {
        if (rrlc.isDeleted == ResourceRateLimitConfig.DELETED) {
            ResourceRateLimitConfig removedRrlc = resourceRateLimitConfigMap.remove(rrlc.resource);
            log.info("remove " + removedRrlc);
        } else {
            ResourceRateLimitConfig existRrlc = resourceRateLimitConfigMap.get(rrlc.resource);
            resourceRateLimitConfigMap.put(rrlc.resource, rrlc);
            if (existRrlc == null) {
                log.info("add " + rrlc);
            } else {
                log.info("update " + existRrlc + " with " + rrlc);
            }
        }
    }

    public ResourceRateLimitConfig getResourceRateLimitConfig(String resource) {
        return resourceRateLimitConfigMap.get(resource);
    }

    public Map<String, ResourceRateLimitConfig> getResourceRateLimitConfigMap() {
        return resourceRateLimitConfigMap;
    }
}
