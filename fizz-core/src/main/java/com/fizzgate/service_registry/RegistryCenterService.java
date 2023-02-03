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

package com.fizzgate.service_registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fizzgate.config.AggregateRedisConfig;
import com.fizzgate.config.SystemConfig;
import com.fizzgate.util.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Service
public class RegistryCenterService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryCenterService.class);

    private Map<String, RegistryCenter> registryCenterMap = new HashMap<>();

    @Resource
    private ReactiveWebServerApplicationContext applicationContext;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private SystemConfig systemConfig;

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

    public Result<?> initRegistryCenter() {
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
                                                     RegistryCenter currentRegistryCenter = registryCenterMap.get(rc.name);
                                                     if (currentRegistryCenter == null) {
                                                         register(rc);
                                                     } else if (!rc.equals(currentRegistryCenter)) {
                                                         deregister(currentRegistryCenter);
                                                         register(rc);
                                                     }
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

    private void deregister(RegistryCenter rc) throws Throwable {
        FizzServiceRegistration fizzServiceRegistration = rc.getFizzServiceRegistration();
        Throwable error = null;
        try {
            fizzServiceRegistration.deregister();
            registryCenterMap.remove(rc.name);
        } catch (Throwable t) {
            LOGGER.error("deregister {}", rc, t);
            error = t;
        } finally {
            try {
                fizzServiceRegistration.close();
            } catch (Throwable t) {
                LOGGER.error("close {}", rc, t);
                error = t;
            }
        }
        if (error != null) {
            if (systemConfig.isFastFailWhenRegistryCenterDown()) {
                throw error;
            } else {
                LOGGER.warn("fail to deregister {}, fast fail when registry center down is false, so continue", rc);
            }
        }
    }

    private void register(RegistryCenter rc) throws Throwable {
        rc.initFizzServiceRegistration(applicationContext);
        FizzServiceRegistration fizzServiceRegistration = rc.getFizzServiceRegistration();
        try {
            fizzServiceRegistration.register();
            registryCenterMap.put(rc.name, rc);
        } catch (Throwable t0) {
            Throwable error = t0;
            LOGGER.error("register {}", rc, t0);
            try {
                fizzServiceRegistration.close();
            } catch (Throwable t1) {
                error = t1;
                LOGGER.error("close {}", rc, t1);
            }
            if (systemConfig.isFastFailWhenRegistryCenterDown()) {
                throw error;
            } else {
                LOGGER.warn("fail to register {}, fast fail when registry center down is false, so continue", rc);
            }
        }
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
                          RegistryCenter prev = registryCenterMap.get(rc.name);
                          if (rc.isDeleted) {
                              deregister(prev);
                          } else {
                              if (prev != null) {
                                  deregister(prev);
                              }
                              register(rc);
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
        RegistryCenter rc = registryCenterMap.get(registryCenter);
        if (rc == null) {
            throw Utils.runtimeExceptionWithoutStack(registryCenter + " not exists");
        }
        return rc.getInstance(service);
    }

    public static String getServiceNameSpace(String registryCenter, String service) {
        if (registryCenter == null) {
            return service;
        }
        StringBuilder b = ThreadContext.getStringBuilder(ThreadContext.sb0);
        return b.append(registryCenter).append(Consts.S.COMMA).append(service).toString();
    }
}
