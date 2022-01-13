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

package we.stats.circuitbreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.stats.FlowStat;
import we.util.JacksonUtils;
import we.util.ResourceIdUtils;
import we.util.Result;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Component
public class CircuitBreakManager {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakManager.class);

    private Map<String/*child resource*/, String/*parent resource*/> parentResourceMap = new HashMap<>(128);

    private Map<String/*resource*/, CircuitBreaker> circuitBreakerMap = new HashMap<>(64);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() {
        Result<?> result = initCircuitBreakers();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnCircuitBreakerChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        log.info("init parentResourceMap: {}", parentResourceMap);
        // schedule();
    }

    private Result<?> initCircuitBreakers() {
        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> circuitBreakerConfigs = rt.opsForHash().entries("fizz_degrade_rule");
        circuitBreakerConfigs.collectList()
                             .defaultIfEmpty(Collections.emptyList())
                             .flatMap(
                                     es -> {
                                         if (!es.isEmpty()) {
                                             String json = null;
                                             try {
                                                 for (Map.Entry<Object, Object> e : es) {
                                                     json = (String) e.getValue();
                                                     CircuitBreaker cb = JacksonUtils.readValue(json, CircuitBreaker.class);
                                                     circuitBreakerMap.put(cb.resource, cb);
                                                     updateParentResourceMap(cb);
                                                     log.info("init circuit breaker {}", cb);
                                                 }
                                             } catch (Throwable t) {
                                                 result.code = Result.FAIL;
                                                 result.msg  = "init circuit breaker error, json: " + json;
                                                 result.t    = t;
                                             }
                                         } else {
                                             log.info("no circuit breaker config");
                                         }
                                         return Mono.empty();
                                     }
                             )
                             .onErrorReturn(
                                     throwable -> {
                                         result.code = Result.FAIL;
                                         result.msg  = "init circuit breaker error";
                                         result.t    = throwable;
                                         return true;
                                     },
                                     result
                             )
                             .block();
        return result;
    }

    private Result<?> lsnCircuitBreakerChange() {
        Result<?> result = Result.succ();
        String channel = "fizz_degrade_rule_channel";
        rt.listenToChannel(channel)
          .doOnError(
                  t -> {
                      result.code = Result.FAIL;
                      result.msg  = "lsn error, channel: " + channel;
                      result.t    = t;
                      log.error("lsn channel {} error", channel, t);
                  }
          )
          .doOnSubscribe(
                  s -> {
                      log.info("success to lsn on {}", channel);
                  }
          )
          .doOnNext(
                  msg -> {
                      String message = msg.getMessage();
                      try {
                          CircuitBreaker cb = JacksonUtils.readValue(message, CircuitBreaker.class);
                          if (cb.isDeleted) {
                              circuitBreakerMap.remove(cb.resource);
                              log.info("remove circuit breaker: {}", cb);
                          } else {
                              circuitBreakerMap.put(cb.resource, cb);
                              log.info("update circuit breaker: {}", cb);
                          }
                          updateParentResourceMap(cb);
                          log.info("update parentResourceMap: {}", parentResourceMap);
                      } catch (Throwable t) {
                          log.error("update circuit breaker error, {}", message, t);
                      }
                  }
          )
          .subscribe();
        return result;
    }

    private void updateParentResourceMap(CircuitBreaker cb) {
        String parentResource = null;
        if (cb.isDeleted) {
            if (cb.type == CircuitBreaker.Type.PATH) {
                parentResourceMap.remove(cb.resource);
                parentResource = ResourceIdUtils.buildResourceId(null, null, null, cb.service, null);
                parentResourceMap.remove(parentResource);
            } else if (cb.type == CircuitBreaker.Type.SERVICE) {
                parentResourceMap.remove(cb.resource);
            }
            return;
        }
        if (cb.type == CircuitBreaker.Type.PATH) {
            parentResource = ResourceIdUtils.buildResourceId(null, null, null, cb.service, null);
            parentResourceMap.put(cb.resource, parentResource);
        }
        if (cb.type != CircuitBreaker.Type.SERVICE_DEFAULT) {
            if (parentResource == null) {
                parentResourceMap.put(cb.resource, ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);
            } else {
                parentResourceMap.put(parentResource, ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);
            }
        }
    }

    public boolean permit(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String service, String path) {
        String resource = ResourceIdUtils.buildResourceId(null, null, null, service, path);
        return permit(exchange, currentTimeWindow, flowStat, resource);
    }

    public boolean permit(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String resource) {
        while (true) {
            CircuitBreaker cb = circuitBreakerMap.get(resource);
            if (cb != null) {
                if (cb.type != CircuitBreaker.Type.SERVICE_DEFAULT || cb.serviceDefaultEnable) {
                    return cb.permit(exchange, currentTimeWindow, flowStat);
                }
            }
            resource = parentResourceMap.get(resource);
            if (resource == null) {
                return true;
            }
        }
    }

    public void correctCircuitBreakerState4error(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String service, String path) {
        String resource = ResourceIdUtils.buildResourceId(null, null, null, service, path);
        correctCircuitBreakerState4error(exchange, currentTimeWindow, flowStat, resource);
    }

    public void correctCircuitBreakerState4error(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String resource) {
        while (true) {
            CircuitBreaker cb = circuitBreakerMap.get(resource);
            if (cb != null) {
                if (cb.type != CircuitBreaker.Type.SERVICE_DEFAULT || cb.serviceDefaultEnable) {
                    cb.correctCircuitBreakerStateAsError(currentTimeWindow, flowStat);
                    return;
                }
            }
            resource = parentResourceMap.get(resource);
            if (resource == null) {
                return;
            }
        }
    }

    public CircuitBreaker getCircuitBreaker(String resource) {
        return circuitBreakerMap.get(resource);
    }

    public Map<String, CircuitBreaker> getCircuitBreakerMap() {
        return circuitBreakerMap;
    }

    public Map<String, String> getParentResourceMap() {
        return parentResourceMap;
    }
}
