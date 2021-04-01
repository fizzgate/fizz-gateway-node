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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.config.AggregateRedisConfig;
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.NetworkUtils;
import we.util.ReactorUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author hongqiaowei
 */

@Service
public class GatewayGroupService {

    private static final Logger log = LoggerFactory.getLogger(GatewayGroupService.class);

    private static final String fizzGatewayGroup        = "fizz_gateway_group";

    private static final String fizzGatewayGroupChannel = "fizz_gateway_group_channel";

    public  Map<String,  GatewayGroup>  gatewayGroupMap        = new HashMap<>(6);

    private Map<Integer, GatewayGroup>  oldGatewayGroupMap     = new HashMap<>(6);

    public  Set<String>                 currentGatewayGroupSet = Stream.of(GatewayGroup.DEFAULT).collect(Collectors.toSet());

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        final Throwable[] throwable = new Throwable[1];
        Throwable error = Mono.just(Objects.requireNonNull(rt.opsForHash().entries(fizzGatewayGroup)
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
                        GatewayGroup gg = JacksonUtils.readValue(json, GatewayGroup.class);
                        oldGatewayGroupMap.put(gg.id, gg);
                        updateGatewayGroupMap(gg);
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
                    return lsnGatewayGroupChange();
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            throw error;
        }
    }

    private Mono<Throwable> lsnGatewayGroupChange() {
        final Throwable[] throwable = new Throwable[1];
        final boolean[] b = {false};
        rt.listenToChannel(fizzGatewayGroupChannel).doOnError(t -> {
            throwable[0] = t;
            b[0] = false;
            log.error("lsn " + fizzGatewayGroupChannel, t);
        }).doOnSubscribe(
                s -> {
                    b[0] = true;
                    log.info("success to lsn on " + fizzGatewayGroupChannel);
                }
        ).doOnNext(msg -> {
            String json = msg.getMessage();
            log.info(json, LogService.BIZ_ID, "gg" + System.currentTimeMillis());
            try {
                GatewayGroup gg = JacksonUtils.readValue(json, GatewayGroup.class);
                GatewayGroup r = oldGatewayGroupMap.remove(gg.id);
                if (gg.isDeleted != GatewayGroup.DELETED && r != null) {
                    gatewayGroupMap.remove(r.group);
                }
                updateGatewayGroupMap(gg);
                if (gg.isDeleted != GatewayGroup.DELETED) {
                    oldGatewayGroupMap.put(gg.id, gg);
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

    private void updateGatewayGroupMap(GatewayGroup gg) {
        if (gg.isDeleted == GatewayGroup.DELETED) {
            GatewayGroup r = gatewayGroupMap.remove(gg.group);
            log.info("remove " + r);
        } else {
            GatewayGroup existGatewayGroup = gatewayGroupMap.get(gg.group);
            gatewayGroupMap.put(gg.group, gg);
            if (existGatewayGroup == null) {
                log.info("add " + gg);
            } else {
                log.info("update " + existGatewayGroup + " with " + gg);
            }
        }
        updateCurrentGatewayGroupSet();
    }

    private void updateCurrentGatewayGroupSet() {
        String ip = NetworkUtils.getServerIp();
        currentGatewayGroupSet.clear();
        gatewayGroupMap.forEach(
                (k, gg) -> {
                    if (gg.gateways.contains(ip)) {
                        currentGatewayGroupSet.add(gg.group);
                    }
                }
        );
        if (currentGatewayGroupSet.isEmpty()) {
            currentGatewayGroupSet.add(GatewayGroup.DEFAULT);
        }
    }

    public boolean currentGatewayGroupIn(Set<String> gatewayGroups) {
        for (String cgg : currentGatewayGroupSet) {
            if (gatewayGroups.contains(cgg)) {
                return true;
            }
        }
        return false;
    }
}
