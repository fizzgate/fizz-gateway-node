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

package we.service_registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.util.Consts;
import we.util.JacksonUtils;
import we.util.Result;
import we.util.ThreadContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Service
public class RegistryCenterService {

    private static final Logger log = LoggerFactory.getLogger(RegistryCenterService.class);

    private Map<String, RegistryCenter> registryCenterMap = new HashMap<>();

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() {
        Result<?> result = initRegistryCenter();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnRegistryCenterChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    private Result<?> initRegistryCenter() {
        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> registryCenterEntries = rt.opsForHash().entries("fizz_registry");
        registryCenterEntries.collectList()
                             .defaultIfEmpty(Collections.emptyList())
                             .flatMap(
                                     es -> {
                                         if (!es.isEmpty()) {
                                             String json = null;
                                             try {
                                                 for (Map.Entry<Object, Object> e : es) {
                                                     json = (String) e.getValue();
                                                     RegistryCenter rc = JacksonUtils.readValue(json, RegistryCenter.class);
                                                     registryCenterMap.put(rc.name, rc);
                                                     log.info("init registry center {}", rc.name);
                                                 }
                                             } catch (Throwable t) {
                                                 result.code = Result.FAIL;
                                                 result.msg  = "init registry center error, json: " + json;
                                                 result.t    = t;
                                             }
                                         } else {
                                             log.info("no registry center");
                                         }
                                         return Mono.empty();
                                     }
                             )
                             .onErrorReturn(
                                     throwable -> {
                                         result.code = Result.FAIL;
                                         result.msg  = "init registry center error";
                                         result.t    = throwable;
                                         return true;
                                     },
                                     result
                             )
                             .block();
        return result;
    }

    private Result<?> lsnRegistryCenterChange() {
        Result<?> result = Result.succ();
        String channel = "fizz_registry_channel";
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
                          RegistryCenter rc = JacksonUtils.readValue(message, RegistryCenter.class);
                          if (rc.isDeleted) {
                              registryCenterMap.remove(rc.name);
                              log.info("remove registry center {}", rc.name);
                          } else {
                              registryCenterMap.put(rc.name, rc);
                              log.info("update registry center {}", rc.name);
                          }
                      } catch (Throwable t) {
                          log.error("update registry center error, {}", message, t);
                      }
                  }
          )
          .subscribe();
        return result;
    }

    public RegistryCenter getRegistryCenter(String name) {
        return registryCenterMap.get(name);
    }

    public String getInstance(String registryCenter, String service) {
        return registryCenterMap.get(registryCenter).getInstance(service);
    }

    public static String getServiceNameSpace(String registryCenter, String service) {
        if (registryCenter == null) {
            return service;
        }
        StringBuilder b = ThreadContext.getStringBuilder(ThreadContext.sb0);
        return b.append(registryCenter).append(Consts.S.COMMA).append(service).toString();
    }
}
