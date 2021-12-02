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

package we.dedicated_line;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.config.SystemConfig;
import we.util.JacksonUtils;
import we.util.Result;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = SystemConfig.FIZZ_DEDICATED_LINE_CLIENT_ENABLE, havingValue = "true")
@Service
public class DedicatedLineInfoService {

    private static final Logger log = LoggerFactory.getLogger(DedicatedLineInfoService.class);

    private Map<String , DedicatedLineInfo> serviceDedicatedLineInfoMap = new HashMap<>(32);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        Result<?> result = initDedicatedLineInfo();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnApiPairingInfoChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    private Result<?> initDedicatedLineInfo() {
        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> resources = rt.opsForHash().entries("fizz_dedicated_line_info");
        resources.collectList()
                 .defaultIfEmpty(Collections.emptyList())
                 .flatMap(
                         es -> {
                             if (!es.isEmpty()) {
                                 String json = null;
                                 try {
                                     for (Map.Entry<Object, Object> e : es) {
                                         json = (String) e.getValue();
                                         DedicatedLineInfo info = JacksonUtils.readValue(json, DedicatedLineInfo.class);
                                         for (String service : info.services) {
                                             serviceDedicatedLineInfoMap.put(service, info);
                                         }
                                         log.info("init dedicated line info: {}", info);
                                     }
                                 } catch (Throwable t) {
                                     result.code = Result.FAIL;
                                     result.msg  = "init dedicated line info error, info: " + json;
                                     result.t    = t;
                                 }
                             } else {
                                 log.info("no dedicated line info");
                             }
                             return Mono.empty();
                         }
                 )
                 .onErrorReturn(
                         throwable -> {
                             result.code = Result.FAIL;
                             result.msg  = "init dedicated line info error";
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
        String channel = "fizz_dedicated_line_info_channel";
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
                          DedicatedLineInfo info = JacksonUtils.readValue(message, DedicatedLineInfo.class);
                          if (info.isDeleted) {
                              for (String service : info.services) {
                                  serviceDedicatedLineInfoMap.remove(service);
                              }
                              log.info("remove dedicated line info: {}", info);
                          } else {
                              for (String service : info.services) {
                                  serviceDedicatedLineInfoMap.put(service, info);
                              }
                              log.info("update dedicated line info: {}", info);
                          }
                      } catch (Throwable t) {
                          log.error("update dedicated line info error, {}", message, t);
                      }
                  }
          )
          .subscribe();
        return result;
    }

    public DedicatedLineInfo get(String service) {
        return serviceDedicatedLineInfoMap.get(service);
    }
}
