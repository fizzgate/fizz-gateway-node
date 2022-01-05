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
package we.stats.degrade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.util.JacksonUtils;
import we.util.Result;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Degrade rule service
 *
 * @author zhongjie
 */
@Service
@Slf4j
public class DegradeRuleService {

    /**
     * Redis degrade rule change channel
     */
    private static final String DEGRADE_RULE_CHANNEL = "fizz_degrade_rule_channel";
    /**
     * redis degrade rule info hash key
     */
    private static final String DEGRADE_RULE_HASH_KEY = "fizz_degrade_rule";


    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    private Map<String, DegradeRule> resourceId2DegradeRuleMap = new ConcurrentHashMap<>(32);
    private Map<Long, DegradeRule> id2DegradeRuleMap = new ConcurrentHashMap<>(32);

    @PostConstruct
    public void init() {
        Result<?> result = initDegradeRule();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnDegradeRuleChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    public void refreshLocalCache() throws Throwable {
        this.initDegradeRule();
    }

    private Result<?> initDegradeRule() {
        Result<?> result = Result.succ();

        Map<String, DegradeRule> resourceId2DegradeRuleMapTmp = new ConcurrentHashMap<>(32);
        Map<Long, DegradeRule> id2DegradeRuleMapTmp = new ConcurrentHashMap<>(32);

        Flux<Map.Entry<Object, Object>> degradeRuleEntries = rt.opsForHash().entries(DEGRADE_RULE_HASH_KEY);
        degradeRuleEntries.collectList()
                .defaultIfEmpty(Collections.emptyList())
                .flatMap(es -> {
                            if (!es.isEmpty()) {
                                String json = null;
                                try {
                                    for (Map.Entry<Object, Object> e : es) {
                                        json = (String) e.getValue();
                                        DegradeRule degradeRule = JacksonUtils.readValue(json, DegradeRule.class);
                                        resourceId2DegradeRuleMapTmp.put(degradeRule.getResourceId(), degradeRule);
                                        id2DegradeRuleMapTmp.put(degradeRule.getId(), degradeRule);
                                        log.info("init degrade rule: {}", json);
                                    }
                                } catch (Throwable t) {
                                    result.code = Result.FAIL;
                                    result.msg = "init degrade rule error, json: " + json;
                                    result.t = t;
                                }
                            } else {
                                log.info("no degrade rule");
                            }
                            return Mono.empty();
                        }
                )
                .onErrorReturn(
                        throwable -> {
                            result.code = Result.FAIL;
                            result.msg = "init degrade rule error";
                            result.t = throwable;
                            return true;
                        },
                        result
                )
                .block();
        resourceId2DegradeRuleMap = resourceId2DegradeRuleMapTmp;
        id2DegradeRuleMap = id2DegradeRuleMapTmp;
        return result;
    }

    private Result<?> lsnDegradeRuleChange() {
        Result<?> result = Result.succ();
        rt.listenToChannel(DEGRADE_RULE_CHANNEL)
                .doOnError(
                        t -> {
                            result.code = Result.FAIL;
                            result.msg = "lsn error, channel: " + DEGRADE_RULE_CHANNEL;
                            result.t = t;
                            log.error("lsn channel {} error", DEGRADE_RULE_CHANNEL, t);
                        }
                )
                .doOnSubscribe(
                        s -> log.info("success to lsn on {}", DEGRADE_RULE_CHANNEL)
                )
                .doOnNext(
                        msg -> {
                            String message = msg.getMessage();
                            try {
                                DegradeRule degradeRule = JacksonUtils.readValue(message, DegradeRule.class);
                                if (degradeRule.isDeleted()) {
                                    DegradeRule remove = id2DegradeRuleMap.remove(degradeRule.getId());
                                    if (remove != null) {
                                        resourceId2DegradeRuleMap.remove(remove.getResourceId());
                                    }
                                    log.info("remove degrade rule {}", message);
                                } else {
                                    DegradeRule previous = id2DegradeRuleMap.put(degradeRule.getId(), degradeRule);
                                    if (previous != null) {
                                        if (!previous.getResourceId().equals(degradeRule.getResourceId())) {
                                            resourceId2DegradeRuleMap.remove(previous.getResourceId());
                                        }
                                    }
                                    resourceId2DegradeRuleMap.put(degradeRule.getResourceId(), degradeRule);
                                    log.info("update degrade rule {}", message);
                                }
                            } catch (Throwable t) {
                                log.error("update degrade rule error, {}", message, t);
                            }
                        }
                )
                .subscribe();
        return result;
    }
}
