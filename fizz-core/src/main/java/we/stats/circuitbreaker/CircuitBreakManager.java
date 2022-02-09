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
import we.flume.clients.log4j2appender.LogService;
import we.stats.FlowStat;
import we.util.JacksonUtils;
import we.util.ResourceIdUtils;
import we.util.Result;
import we.util.WebUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author hongqiaowei
 */

@Component
public class CircuitBreakManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreakManager.class);

//  private Map<String/*child resource*/, String/*parent resource*/> parentResourceMap = new HashMap<>(128);

    private final Map<Long,   CircuitBreaker> circuitBreakerMap                 = new HashMap<>(64);

    private final Map<String, CircuitBreaker> resource2circuitBreakerMap        = new HashMap<>(64);

    private final Set<String>                 circuitBreakersFromServiceDefault = new HashSet<>(64);

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
        // LOGGER.info("init parentResourceMap: {}", parentResourceMap);
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
                                                     circuitBreakerMap.put(cb.id, cb);
                                                     resource2circuitBreakerMap.put(cb.resource, cb);
                                                     // updateParentResourceMap(cb);
                                                     LOGGER.info("init circuit breaker {}", cb);
                                                 }
                                             } catch (Throwable t) {
                                                 result.code = Result.FAIL;
                                                 result.msg  = "init circuit breaker error, json: " + json;
                                                 result.t    = t;
                                             }
                                         } else {
                                             LOGGER.info("no circuit breaker config");
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
                      LOGGER.error("lsn channel {} error", channel, t);
                  }
          )
          .doOnSubscribe(
                  s -> {
                      LOGGER.info("success to lsn on {}", channel);
                  }
          )
          .doOnNext(
                  msg -> {
                      String message = msg.getMessage();
                      try {
                          CircuitBreaker cb = JacksonUtils.readValue(message, CircuitBreaker.class);
                          if (cb.isDeleted) {
                              circuitBreakerMap.remove(cb.id);
                              resource2circuitBreakerMap.remove(cb.resource);
                              LOGGER.info("remove circuit breaker: {}", cb);
                          } else {
                              CircuitBreaker prev = circuitBreakerMap.get(cb.id);
                              if (prev != null) {
                                  resource2circuitBreakerMap.remove(prev.resource);
                              }
                              circuitBreakerMap.put(cb.id, cb);
                              resource2circuitBreakerMap.put(cb.resource, cb);
                              circuitBreakersFromServiceDefault.remove(cb.resource);
                              LOGGER.info("update circuit breaker: {}", cb);
                          }
                          // updateParentResourceMap(cb);
                          // LOGGER.info("update parentResourceMap: {}", parentResourceMap);
                          if (cb.type == CircuitBreaker.Type.SERVICE_DEFAULT) {
                              // if (cb.isDeleted || !cb.serviceDefaultEnable) {
                                  for (String resource : circuitBreakersFromServiceDefault) {
                                      resource2circuitBreakerMap.remove(resource);
                                  }
                                  circuitBreakersFromServiceDefault.clear();
                              // }
                          }
                      } catch (Throwable t) {
                          LOGGER.error("update circuit breaker error, {}", message, t);
                      }
                  }
          )
          .subscribe();
        return result;
    }

    /*private void updateParentResourceMap(CircuitBreaker cb) {
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
    }*/

    public boolean permit(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String service, String path) {
        String resource = ResourceIdUtils.buildResourceId(null, null, null, service, path);
        // return permit(exchange, currentTimeWindow, flowStat, resource);
        CircuitBreaker cb = resource2circuitBreakerMap.get(resource);
        if (cb == null) {
            resource = ResourceIdUtils.buildResourceId(null, null, null, service, null);
            cb = resource2circuitBreakerMap.get(resource);
            if (cb == null) {
                cb = resource2circuitBreakerMap.get(ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);
                if (cb != null && cb.serviceDefaultEnable) {
                    cb = buildCircuitBreakerFromServiceDefault(service, resource);
                } else {
                    cb = null;
                }
            }
        }
        if (cb == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no circuit breaker for {} {}", service, path, LogService.BIZ_ID, WebUtils.getTraceId(exchange));
            }
            return true;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("circuit breaker for {} {} is {}", service, path, cb, LogService.BIZ_ID, WebUtils.getTraceId(exchange));
        }
        return cb.permit(exchange, currentTimeWindow, flowStat);
    }

    private CircuitBreaker buildCircuitBreakerFromServiceDefault(String service, String resource) {
        CircuitBreaker serviceDefaultCircuitBreaker = resource2circuitBreakerMap.get(ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);

        CircuitBreaker cb = new CircuitBreaker();
        cb.type                 = CircuitBreaker.Type.SERVICE;
        cb.service              = service;
        cb.serviceDefaultEnable = true;
        cb.resource             = resource;
        cb.breakStrategy        = serviceDefaultCircuitBreaker.breakStrategy;
        cb.errorRatioThreshold  = serviceDefaultCircuitBreaker.errorRatioThreshold;
        cb.totalErrorThreshold  = serviceDefaultCircuitBreaker.totalErrorThreshold;
        cb.minRequests          = serviceDefaultCircuitBreaker.minRequests;
        cb.monitorDuration      = serviceDefaultCircuitBreaker.monitorDuration;
        cb.breakDuration        = serviceDefaultCircuitBreaker.breakDuration;
        cb.resumeStrategy       = serviceDefaultCircuitBreaker.resumeStrategy;
        if (cb.resumeStrategy == CircuitBreaker.ResumeStrategy.GRADUAL) {
            cb.resumeDuration = serviceDefaultCircuitBreaker.resumeDuration;
            cb.initGradualResumeTimeWindowContext();
        }
        cb.responseContentType = serviceDefaultCircuitBreaker.responseContentType;
        cb.responseContent     = serviceDefaultCircuitBreaker.responseContent;
        cb.stateStartTime      = serviceDefaultCircuitBreaker.stateStartTime;

        resource2circuitBreakerMap.put(resource, cb);
        circuitBreakersFromServiceDefault.add(resource);

        return cb;
    }

    /*public boolean permit(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String resource) {
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
    }*/

    public void correctCircuitBreakerStateAsError(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String service, String path) {
        String resource = ResourceIdUtils.buildResourceId(null, null, null, service, path);
        // correctCircuitBreakerState4error(exchange, currentTimeWindow, flowStat, resource);
        CircuitBreaker cb = resource2circuitBreakerMap.get(resource);
        if (cb == null) {
            resource = ResourceIdUtils.buildResourceId(null, null, null, service, null);
            cb = resource2circuitBreakerMap.get(resource);
            if (cb == null) {
                cb = resource2circuitBreakerMap.get(ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);
                if (cb != null && cb.serviceDefaultEnable) {
                    cb = buildCircuitBreakerFromServiceDefault(service, resource);
                } else {
                    cb = null;
                }
            }
        }
        if (cb == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no circuit breaker for {} {}", service, path, LogService.BIZ_ID, WebUtils.getTraceId(exchange));
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("circuit breaker for {} {} is {}", service, path, cb, LogService.BIZ_ID, WebUtils.getTraceId(exchange));
            }
            cb.correctCircuitBreakerStateAsError(currentTimeWindow, flowStat);
        }
    }

    /*public void correctCircuitBreakerState4error(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat, String resource) {
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
    }*/

    public CircuitBreaker getCircuitBreaker(String resource) {
        return resource2circuitBreakerMap.get(resource);
    }

    public Map<String, CircuitBreaker> getResource2circuitBreakerMap() {
        return resource2circuitBreakerMap;
    }

    /*public Map<String, String> getParentResourceMap() {
        return parentResourceMap;
    }*/
}
