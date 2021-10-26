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
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.Fizz;
import we.config.AggregateRedisConfig;
import we.config.SystemConfig;
import we.plugin.auth.ApiConfig;
import we.util.JacksonUtils;
import we.util.ReactiveResult;
import we.util.Result;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = SystemConfig.FIZZ_API_PAIRING_SERVER_ENABLE, havingValue = "true")
@Service
public class ApiPairingDocSetService {

    private static final Logger log = LoggerFactory.getLogger(ApiPairingDocSetService.class);

    private Map<Integer/* doc set id */, ApiPairingDocSet>                                docSetMap                   = new HashMap<>(64);

    private Map<String /*   app id   */, Set<ApiPairingDocSet>>                           appDocSetMap                = new HashMap<>(64);

    private Map<String /*  service   */, Set<ApiPairingDocSet>>                           serviceExistsInDocSetMap    = new HashMap<>(64);

    private Map<String /*    path    */, Map<Object /* method */, Set<ApiPairingDocSet>>> pathMethodExistsInDocSetMap = new HashMap<>(64);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        Result<?> result = initApiPairingDocSet();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnApiPairingDocSetChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    private Result<?> initApiPairingDocSet() {
        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> resources = rt.opsForHash().entries("fizz_api_pairing_doc");
        resources.collectList()
                 .defaultIfEmpty(Collections.emptyList())
                 .flatMap(
                         es -> {
                             if (Fizz.context != null) {
                                 String json = null;
                                 try {
                                     for (Map.Entry<Object, Object> e : es) {
                                         json = (String) e.getValue();
                                         ApiPairingDocSet docSet = JacksonUtils.readValue(json, ApiPairingDocSet.class);
                                         updateDocSetDataStruct(docSet);
                                     }
                                 } catch (Throwable t) {
                                     result.code = Result.FAIL;
                                     result.msg  = "init api pairing doc set error, doc set: " + json;
                                     result.t    = t;
                                 }
                             }
                             return Mono.empty();
                         }
                 )
                 .onErrorReturn(
                         throwable -> {
                             result.code = Result.FAIL;
                             result.msg  = "init api pairing doc set error";
                             result.t    = throwable;
                             return true;
                         },
                         result
                 )
                 .block();
        return result;
    }

    private Result<?> lsnApiPairingDocSetChange() {
        Result<?> result = Result.succ();
        String channel = "fizz_api_pairing_doc_channel";
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
                              ApiPairingDocSet docSet = JacksonUtils.readValue(message, ApiPairingDocSet.class);
                              updateDocSetDataStruct(docSet);
                          } catch (Throwable t) {
                              log.error("update api pairing doc set error, {}", message, t);
                          }
                      }
                  }
          )
          .subscribe();
        return result;
    }

    private void updateDocSetDataStruct(ApiPairingDocSet docSet) {
        if (docSet.isDeleted == ApiPairingDocSet.DELETED) {
            docSetMap.remove(docSet.id);

            for (String appId : docSet.appIds) {
                Set<ApiPairingDocSet> dss = appDocSetMap.get(appId);
                if (dss != null) {
                    dss.remove(docSet);
                    if (dss.isEmpty()) {
                        appDocSetMap.remove(appId);
                    }
                }
            }

            for (ApiPairingDoc doc : docSet.docs) {
                Set<ApiPairingDocSet> dss = serviceExistsInDocSetMap.get(doc.service);
                if (dss != null) {
                    dss.remove(docSet);
                    if (dss.isEmpty()) {
                        serviceExistsInDocSetMap.remove(doc.service);
                    }
                }
                for (Api api : doc.apis) {
                    Map<Object, Set<ApiPairingDocSet>> methodDocSetMap = pathMethodExistsInDocSetMap.get(api.path);
                    if (methodDocSetMap != null) {
                        dss = methodDocSetMap.get(api.method);
                        if (dss != null) {
                            dss.remove(docSet);
                            if (dss.isEmpty()) {
                                methodDocSetMap.remove(api.method);
                                if (methodDocSetMap.isEmpty()) {
                                    pathMethodExistsInDocSetMap.remove(api.path);
                                }
                            }
                        }
                    }
                }
            }

            log.info("delete doc set: {}", docSet);

        } else {
            docSetMap.put(docSet.id, docSet);
            docSet.appIds.forEach(
                    appId -> {
                        Set<ApiPairingDocSet> dss = appDocSetMap.get(appId);
                        if (dss == null) {
                            dss = new HashSet<>();
                            appDocSetMap.put(appId, dss);
                        }
                        dss.add(docSet);
                    }
            );
            docSet.docs.forEach(
                    doc -> {
                        Set<ApiPairingDocSet> dss = serviceExistsInDocSetMap.get(doc.service);
                        if (dss == null) {
                            dss = new HashSet<>();
                            serviceExistsInDocSetMap.put(doc.service, dss);
                        }
                        dss.add(docSet);
                        for (Api api : doc.apis) {
                            Map<Object, Set<ApiPairingDocSet>> methodDocSetMap = pathMethodExistsInDocSetMap.get(api.path);
                            if (methodDocSetMap == null) {
                                methodDocSetMap = new HashMap<>(8);
                                pathMethodExistsInDocSetMap.put(api.path, methodDocSetMap);
                            }
                            dss = methodDocSetMap.get(api.method);
                            if (dss == null) {
                                dss = new HashSet<>();
                                methodDocSetMap.put(api.method, dss);
                            }
                            dss.add(docSet);
                        }
                    }
            );
            log.info("update doc set: {}", docSet);
        }
    }

    public Map<Integer, ApiPairingDocSet> getDocSetMap() {
        return docSetMap;
    }

    public ApiPairingDocSet get(int id) {
        return docSetMap.get(id);
    }

    public Map<String, Set<ApiPairingDocSet>> getAppDocSetMap() {
        return appDocSetMap;
    }

    public Map<String, Set<ApiPairingDocSet>> getServiceExistsInDocSetMap() {
        return serviceExistsInDocSetMap;
    }

    public Map<String, Map<Object, Set<ApiPairingDocSet>>> getPathMethodExistsInDocSetMap() {
        return pathMethodExistsInDocSetMap;
    }

    public boolean existsDocSetMatch(String appId, HttpMethod method, String service, String path) {
        Set<ApiPairingDocSet> appDocSets = appDocSetMap.get(appId);
        if (appDocSets == null) {
            return false;
        }
        Set<ApiPairingDocSet> serviceDocSets = serviceExistsInDocSetMap.get(service);
        if (serviceDocSets == null) {
            return false;
        }
        Map<Object, Set<ApiPairingDocSet>> methodDocSetMap = pathMethodExistsInDocSetMap.get(path);
        if (methodDocSetMap == null) {
            return false;
        }
        Set<ApiPairingDocSet> pathMethodDocSets = methodDocSetMap.get(method);
        if (pathMethodDocSets == null) {
            pathMethodDocSets = methodDocSetMap.get(ApiConfig.ALL_METHOD);
            if (pathMethodDocSets == null) {
                return false;
            }
        }
        Set<ApiPairingDocSet> s = new HashSet<>(appDocSets);
        s.retainAll(serviceDocSets);
        if (s.isEmpty()) {
            return false;
        }
        s.retainAll(pathMethodDocSets);
        if (s.isEmpty()) {
            return false;
        }
        return true;
    }
}
