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

package we.api.pairing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.Fizz;
import we.config.AggregateRedisConfig;
import we.util.JacksonUtils;
import we.util.ReactiveResult;
import we.util.Result;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = "fizz.api.pairing.enable", havingValue = "true")
@Service
public class ApiPairingInfoService {

    private static final Logger log = LoggerFactory.getLogger(ApiPairingInfoService.class);

    private Map<String , ApiPairingInfo> serviceApiPairingInfoMap = new HashMap<>(64);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        Result<?> result = initApiPairingInfo();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnApiPairingInfoChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    private Result<?> initApiPairingInfo() {
        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> resources = rt.opsForHash().entries("fizz_api_pairing_info");
        resources.collectList()
                 .defaultIfEmpty(Collections.emptyList())
                 .flatMap(
                         es -> {
                             if (Fizz.context != null) {
                                 String json = null;
                                 try {
                                     for (Map.Entry<Object, Object> e : es) {
                                         json = (String) e.getValue();
                                         ApiPairingInfo info = JacksonUtils.readValue(json, ApiPairingInfo.class);
                                         for (String service : info.services) {
                                             serviceApiPairingInfoMap.put(service, info);
                                         }
                                         log.info("init api pairing info: {}", info);
                                     }
                                 } catch (Throwable t) {
                                     result.code = Result.FAIL;
                                     result.msg  = "init api pairing info error, info: " + json;
                                     result.t    = t;
                                 }
                             }
                             return Mono.empty();
                         }
                 )
                 .onErrorReturn(
                         throwable -> {
                             result.code = Result.FAIL;
                             result.msg  = "init api pairing info error";
                             result.t    = throwable;
                             return true;
                         },
                         result
                 )
                 .block();
        return result;
    }

    private Result<?> lsnApiPairingInfoChange() {
        Result<?> result = Result.succ();
        String channel = "fizz_api_pairing_info_channel";
        rt.listenToChannel(channel)
          .doOnError(
                  t -> {
                      result.code = ReactiveResult.FAIL;
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
                      if (Fizz.context != null) {
                          String message = msg.getMessage();
                          try {
                              ApiPairingInfo info = JacksonUtils.readValue(message, ApiPairingInfo.class);
                              if (info.isDeleted == ApiPairingDocSet.DELETED) {
                                  for (String service : info.services) {
                                      serviceApiPairingInfoMap.remove(service);
                                  }
                                  log.info("remove api pairing info: {}", info);
                              } else {
                                  for (String service : info.services) {
                                      serviceApiPairingInfoMap.put(service, info);
                                  }
                                  log.info("update api pairing info: {}", info);
                              }
                          } catch (Throwable t) {
                              log.error("update api pairing info error, {}", message, t);
                          }
                      }
                  }
          )
          .subscribe();
        return result;
    }

    public Map<String, ApiPairingInfo> getServiceApiPairingInfoMap() {
        return serviceApiPairingInfoMap;
    }

    public ApiPairingInfo get(String service) {
        return serviceApiPairingInfoMap.get(service);
    }
}
