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

package we.global_resource;

import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.Fizz;
import we.config.AggregateRedisConfig;
import we.fizz.input.PathMapping;
import we.util.JacksonUtils;
import we.util.ReactiveResult;
import we.util.Result;
import we.util.Utils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Service
public class GlobalResourceService {

    private static final Logger log = LoggerFactory.getLogger(GlobalResourceService.class);

    public static ONode resNode;

    private Map<String, GlobalResource> resourceMap = new HashMap<>(64);

    private Map<String, Object>         objectMap   = new HashMap<>(64);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        initGlobalResource().subscribe(
                                    r -> {
                                        if (r.code == ReactiveResult.SUCC) {
                                            lsnGlobalResourceChange().subscribe(
                                                                         res -> {
                                                                             if (res.code == ReactiveResult.FAIL) {
                                                                                 log.error(res.toString());
                                                                                 if (res.t == null) {
                                                                                     throw Utils.runtimeExceptionWithoutStack("lsn global resource error");
                                                                                 }
                                                                                 throw new RuntimeException(res.t);
                                                                             }
                                                                             updateResNode();
                                                                         }
                                                                     );
                                        } else {
                                            log.error(r.toString());
                                            if (r.t == null) {
                                                throw Utils.runtimeExceptionWithoutStack("init global resource error");
                                            }
                                            throw new RuntimeException(r.t);
                                        }
                                    }
                            );
    }

    private void updateResNode() {
        resNode = PathMapping.toONode(objectMap);
        log.info("new object map: {}", JacksonUtils.writeValueAsString(objectMap));
    }

    private Mono<Result<?>> initGlobalResource() {
        Flux<Map.Entry<Object, Object>> dicts = rt.opsForHash().entries("fizz_global_resource");
        dicts.collectList()
             .defaultIfEmpty(Collections.emptyList())
             .flatMap(
                     es -> {
                         if (Fizz.context != null) {
                             for (Map.Entry<Object, Object> e : es) {
                                 String json = (String) e.getValue();
                                 GlobalResource r = JacksonUtils.readValue(json, GlobalResource.class);
                                 resourceMap.put(r.key, r);
                                   objectMap.put(r.key, r.originalVal);
                                 log.info("init global resource: {}", r);
                             }
                         }
                         return Mono.empty();
                     }
             )
             .doOnError(
                     t -> {
                         log.error("init global resource", t);
                     }
             )
             .block();
        return Mono.just(Result.succ());
    }

    private Mono<Result<?>> lsnGlobalResourceChange() {
        Result<?> result = Result.succ();
        String channel = "fizz_global_resource_channel";
        rt.listenToChannel(channel)
          .doOnError(
                  t -> {
                      result.code = ReactiveResult.FAIL;
                      result.t = t;
                      log.error("lsn {}", channel, t);
                  }
          )
          .doOnSubscribe(
                  s -> {
                      log.info("success to lsn on {}", channel);
                  }
          )
          .doOnNext(
                  msg -> {
                      if (Fizz.context != null) {
                          String message = msg.getMessage();
                          try {
                              GlobalResource r = JacksonUtils.readValue(message, GlobalResource.class);
                              if (r.isDeleted == GlobalResource.DELETED) {
                                  resourceMap.remove(r.key);
                                    objectMap.remove(r.key);
                                  log.info("remove global resource {}", r.key);
                              } else {
                                  GlobalResource put = resourceMap.put(r.key, r);
                                                         objectMap.put(r.key, r);
                                  log.info("update global resource {} with {}", put, r);
                              }
                              updateResNode();
                          } catch (Throwable t) {
                              log.error("message: {}", message, t);
                          }
                      }
                  }
          )
          .subscribe();
        return Mono.just(result);
    }

    public Map<String, GlobalResource> getResourceMap() {
        return resourceMap;
    }

    public GlobalResource get(String key) {
        return resourceMap.get(key);
    }
}
