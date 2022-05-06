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
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.util.Consts;
import we.util.JacksonUtils;
import we.util.Result;
import we.util.ThreadContext;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Service
public class RegistryCenterService implements ApplicationListener<ContextRefreshedEvent>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryCenterService.class);

    private Map<String, RegistryCenter> registryCenterMap = new HashMap<>();

    @Resource
    private ReactiveWebServerApplicationContext applicationContext;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
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
                                                     LOGGER.info("init registry center {}", rc);
                                                     rc.initFizzServiceRegistration(applicationContext);
                                                     rc.getFizzServiceRegistration().register();
                                                 }
                                             } catch (Throwable t) {
                                                 result.code = Result.FAIL;
                                                 result.msg  = "init registry center error, json: " + json;
                                                 result.t    = t;
                                             }
                                         } else {
                                             LOGGER.info("no registry center");
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
                          RegistryCenter rc = JacksonUtils.readValue(message, RegistryCenter.class);
                          RegistryCenter prev = null;
                          if (rc.isDeleted) {
                              prev = registryCenterMap.remove(rc.name);
                              LOGGER.info("remove registry center {}", prev);
                              FizzServiceRegistration fizzServiceRegistration = prev.getFizzServiceRegistration();
                              fizzServiceRegistration.deregister();
                              fizzServiceRegistration.close();
                          } else {
                              prev = registryCenterMap.put(rc.name, rc);
                              LOGGER.info("update registry center {} with {}", prev, rc);
                              if (prev != null) {
                                  FizzServiceRegistration fizzServiceRegistration = prev.getFizzServiceRegistration();
                                  fizzServiceRegistration.deregister();
                                  fizzServiceRegistration.close();
                              }
                              rc.initFizzServiceRegistration(applicationContext);
                              rc.getFizzServiceRegistration().register();
                          }
                      } catch (Throwable t) {
                          LOGGER.error("update registry center error, {}", message, t);
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
