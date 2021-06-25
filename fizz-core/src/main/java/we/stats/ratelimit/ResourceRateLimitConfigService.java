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
import we.util.JacksonUtils;
import we.util.ReactorUtils;
import we.util.ThreadContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
        this.init(this::lsnResourceRateLimitConfigChange);
    }

    public void refreshLocalCache() throws Throwable {
        this.init(null);
    }

    private void init(Supplier<Mono<Throwable>> doAfterLoadCache) throws Throwable {
        Map<String, ResourceRateLimitConfig> resourceRateLimitConfigMapTmp = new HashMap<>(32);
        Map<Integer, ResourceRateLimitConfig> oldResourceRateLimitConfigMapTmp = new HashMap<>(32);
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
                        oldResourceRateLimitConfigMapTmp.put(rrlc.id, rrlc);
                        updateResourceRateLimitConfigMap(rrlc, resourceRateLimitConfigMapTmp);
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
                    if (doAfterLoadCache != null) {
                        return doAfterLoadCache.get();
                    } else {
                        return Mono.just(ReactorUtils.EMPTY_THROWABLE);
                    }
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            throw error;
        }
        resourceRateLimitConfigMap = resourceRateLimitConfigMapTmp;
        oldResourceRateLimitConfigMap = oldResourceRateLimitConfigMapTmp;
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
                    resourceRateLimitConfigMap.remove(r.getResourceId());
                }
                updateResourceRateLimitConfigMap(rrlc, resourceRateLimitConfigMap);
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

    private void updateResourceRateLimitConfigMap(ResourceRateLimitConfig rrlc,
                                                  Map<String, ResourceRateLimitConfig> resourceRateLimitConfigMap) {
        if (rrlc.isDeleted == ResourceRateLimitConfig.DELETED) {
            ResourceRateLimitConfig removedRrlc = resourceRateLimitConfigMap.remove(rrlc.getResourceId());
            log.info("remove " + removedRrlc);
        } else {
            ResourceRateLimitConfig existRrlc = resourceRateLimitConfigMap.get(rrlc.getResourceId());
            resourceRateLimitConfigMap.put(rrlc.getResourceId(), rrlc);
            if (existRrlc == null) {
                log.info("add " + rrlc);
            } else {
                log.info("update " + existRrlc + " with " + rrlc);
            }
        }
    }

    public void setReactiveStringRedisTemplate(ReactiveStringRedisTemplate rt) {
        this.rt = rt;
    }

    public ResourceRateLimitConfig getResourceRateLimitConfig(String resource) {
        return resourceRateLimitConfigMap.get(resource);
    }

    public Map<String, ResourceRateLimitConfig> getResourceRateLimitConfigMap() {
        return resourceRateLimitConfigMap;
    }

    // _global, service, app, ip, ip+service
    public void getParentsTo(String resource, List<String> parentList) {
        String app = null, ip = null, node = null, service = null, path = null;
        ResourceRateLimitConfig c = resourceRateLimitConfigMap.get(resource);
        if (c == null) {
            node = ResourceRateLimitConfig.getNode(resource);
            if (node != null && node.equals(ResourceRateLimitConfig.NODE)) {
            } else {
                service = ResourceRateLimitConfig.getService(resource);
                app = ResourceRateLimitConfig.getApp(resource);
                ip = ResourceRateLimitConfig.getIp(resource);
                if (service == null) {
                    parentList.add(ResourceRateLimitConfig.NODE_RESOURCE);
                } else {
                    if (ip == null) {
                        parentList.add(ResourceRateLimitConfig.NODE_RESOURCE);
                    } else {
                        String r = ResourceRateLimitConfig.buildResourceId(null, ip, null, null, null);
                        parentList.add(r);
                        parentList.add(ResourceRateLimitConfig.NODE_RESOURCE);
                    }
                }
            }
            return;
        } else {
            if (c.type == ResourceRateLimitConfig.Type.NODE) {
                return;
            }
            if (c.type == ResourceRateLimitConfig.Type.SERVICE) {
                parentList.add(ResourceRateLimitConfig.NODE_RESOURCE);
                return;
            }
            app = c.app;
            ip = c.ip;
            service = c.service;
            path = c.path;
        }

        StringBuilder b = ThreadContext.getStringBuilder();

        if (app != null) {
            if (path != null) {
                ResourceRateLimitConfig.buildResourceIdTo(b, app, null, null, service, null);
                checkRateLimitConfigAndAddTo(b, parentList);
                ResourceRateLimitConfig.buildResourceIdTo(b, app, null, null, null, null);
                // checkRateLimitConfigAndAddTo(b, parentList);
                to(parentList, b);
            } else if (service != null) {
                ResourceRateLimitConfig.buildResourceIdTo(b, app, null, null, null, null);
                checkRateLimitConfigAndAddTo(b, parentList);
            }
        }

        if (ip != null) {
            if (path != null) {
                ResourceRateLimitConfig.buildResourceIdTo(b, null, ip, null, service, null);
                // checkRateLimitConfigAndAddTo(b, parentList);
                to(parentList, b);
                ResourceRateLimitConfig.buildResourceIdTo(b, null, ip, null, null, null);
                // checkRateLimitConfigAndAddTo(b, parentList);
                to(parentList, b);
            } else if (service != null) {
                ResourceRateLimitConfig.buildResourceIdTo(b, null, ip, null, null, null);
                checkRateLimitConfigAndAddTo(b, parentList);
            }
        }

        if (path != null) {
            ResourceRateLimitConfig.buildResourceIdTo(b, null, null, null, service, null);
            to(parentList, b);
        }

        parentList.add(ResourceRateLimitConfig.NODE_RESOURCE);
    }

    private void to(List<String> parentList, StringBuilder b) {
        parentList.add(b.toString());
        b.delete(0, b.length());
    }

    private void checkRateLimitConfigAndAddTo(StringBuilder resourceStringBuilder, List<String> resourceList) {
        String r = resourceStringBuilder.toString();
        ResourceRateLimitConfig c = resourceRateLimitConfigMap.get(r);
        if (c != null) {
            resourceList.add(r);
        }
        resourceStringBuilder.delete(0, resourceStringBuilder.length());
    }
}
