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

package we.dict;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.FizzAppContext;
import we.config.AggregateRedisConfig;
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
public class DictService {

    private static final Logger log = LoggerFactory.getLogger(DictService.class);

    private Map<String, Dict> dictMap = new HashMap<>(64);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        initDict().subscribe(
                      r -> {
                          if (r.code == ReactiveResult.SUCC) {
                              lsnInitChange().subscribe(
                                                  res -> {
                                                      if (res.code == ReactiveResult.FAIL) {
                                                          log.error(res.toString());
                                                          if (res.t == null) {
                                                              throw Utils.runtimeExceptionWithoutStack("lsn dict error");
                                                          }
                                                          throw new RuntimeException(res.t);
                                                      }
                                                  }
                                             );
                          } else {
                              log.error(r.toString());
                              if (r.t == null) {
                                  throw Utils.runtimeExceptionWithoutStack("init dict error");
                              }
                              throw new RuntimeException(r.t);
                          }
                      }
                  );
    }

    private Mono<Result<?>> initDict() {
        Flux<Map.Entry<Object, Object>> dicts = rt.opsForHash().entries("fizz_dict");
        dicts.collectList()
             .defaultIfEmpty(Collections.emptyList())
             .flatMap(
                     es -> {
                         if (FizzAppContext.appContext != null) {
                             for (Map.Entry<Object, Object> e : es) {
                                 String json = (String) e.getValue();
                                 Dict dict = JacksonUtils.readValue(json, Dict.class);
                                 dictMap.put(dict.key, dict);
                                 log.info("init dict: {}", dict);
                             }
                         }
                         return Mono.empty();
                     }
             )
             .doOnError(
                     t -> {
                         log.error("init dict", t);
                     }
             )
             .block();
        return Mono.just(Result.succ());
    }

    private Mono<Result<?>> lsnInitChange() {
        Result<?> result = Result.succ();
        String channel = "fizz_dict_channel";
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
                      if (FizzAppContext.appContext != null) {
                          String message = msg.getMessage();
                          try {
                              Dict dict = JacksonUtils.readValue(message, Dict.class);
                              if (dict.isDeleted == Dict.DELETED) {
                                  dictMap.remove(dict.key);
                                  log.info("remove dict {}", dict.key);
                              } else {
                                  Dict put = dictMap.put(dict.key, dict);
                                  log.info("update dict {} with {}", put, dict);
                              }
                          } catch (Throwable t) {
                              log.error("message: {}", message, t);
                          }
                      }
                  }
          )
          .subscribe();
        return Mono.just(result);
    }

    public Map<String, Dict> getDictMap() {
        return dictMap;
    }

    public Dict get(String key) {
        return dictMap.get(key);
    }
}
