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

package we.dedicatedline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.config.SystemConfig;
import we.plugin.auth.ApiConfig;
import we.util.JacksonUtils;
import we.util.Result;
import we.util.UrlTransformUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = SystemConfig.FIZZ_DEDICATED_LINE_SERVER_ENABLE, havingValue = "true")
@Service
public class DedicatedLineService {

    private static final Logger log = LoggerFactory.getLogger(DedicatedLineService.class);

    private Map<String, DedicatedLine> dedicatedLineMap = new HashMap<>(32);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        Result<?> result = initDedicatedLine();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnDedicatedLineChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    private Result<?> initDedicatedLine() {
        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> resources = rt.opsForHash().entries("fizz_dedicated_line");
        resources.collectList()
                 .defaultIfEmpty(Collections.emptyList())
                 .flatMap(
                         es -> {
                             if (!es.isEmpty()) {
                                 String json = null;
                                 try {
                                     for (Map.Entry<Object, Object> e : es) {
                                         json = (String) e.getValue();
                                         DedicatedLine dl = JacksonUtils.readValue(json, DedicatedLine.class);
                                         dedicatedLineMap.put(dl.pairCodeId, dl);
                                     }
                                 } catch (Throwable t) {
                                     result.code = Result.FAIL;
                                     result.msg  = "init dedicated line error, json: " + json;
                                     result.t    = t;
                                 }
                             } else {
                                 log.info("no dedicated line");
                             }
                             return Mono.empty();
                         }
                 )
                 .onErrorReturn(
                         throwable -> {
                             result.code = Result.FAIL;
                             result.msg  = "init dedicated line error";
                             result.t    = throwable;
                             return true;
                         },
                         result
                 )
                 .block();
        return result;
    }

    private Result<?> lsnDedicatedLineChange() {
        Result<?> result = Result.succ();
        String channel = "fizz_dedicated_line_channel";
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
                          DedicatedLine dl = JacksonUtils.readValue(message, DedicatedLine.class);
                          if (dl.isDeleted) {
                              dedicatedLineMap.remove(dl.pairCodeId);
                          } else {
                              dedicatedLineMap.put(dl.pairCodeId, dl);
                          }
                      } catch (Throwable t) {
                          log.error("update dedicated line error, {}", message, t);
                      }
                  }
          )
          .subscribe();
        return result;
    }

    public boolean auth(String pairCodeId, HttpMethod method, String service, String path) {
        DedicatedLine dedicatedLine = dedicatedLineMap.get(pairCodeId);
        if (dedicatedLine == null) {
            return false;
        }
        if (dedicatedLine.servicesWithoutApiDocs.contains(service)) {
            return true;
        }
        Map<Object, Set<String>> methodPathsMap = dedicatedLine.apiDocMap.get(service);
        if (methodPathsMap == null) {
            return false;
        }
        Set<String> pathPatterns = methodPathsMap.get(method);
        if (pathPatterns != null) {
            if (pathPatternMatch(path, pathPatterns)) {
                return true;
            }
        }
        pathPatterns = methodPathsMap.get(ApiConfig.ALL_METHOD);
        if (pathPatterns != null) {
            return pathPatternMatch(path, pathPatterns);
        }
        return false;
    }

    private boolean pathPatternMatch(String path, Set<String> pathPatterns) {
        if (pathPatterns.contains(path)) {
            return true;
        }
        for (String pathPattern : pathPatterns) {
            if (UrlTransformUtils.ANT_PATH_MATCHER.match(pathPattern, path)) {
                return true;
            }
        }
        return false;
    }

    public String getSignSecretKey(String pairCodeId) {
        DedicatedLine dedicatedLine = dedicatedLineMap.get(pairCodeId);
        if (dedicatedLine != null) {
            return dedicatedLine.secretKey;
        }
        return null;
    }

    public String getRequestCryptoKey(String pairCodeId) {
        DedicatedLine dedicatedLine = dedicatedLineMap.get(pairCodeId);
        if (dedicatedLine != null) {
            return dedicatedLine.requestCryptoKey;
        }
        return null;
    }

    public String getCustomConfig(String pairCodeId) {
        DedicatedLine dedicatedLine = dedicatedLineMap.get(pairCodeId);
        if (dedicatedLine != null) {
            return dedicatedLine.customConfig;
        }
        return null;
    }
}
