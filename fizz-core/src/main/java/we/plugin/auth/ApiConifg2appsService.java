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

package we.plugin.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.flume.clients.log4j2appender.LogService;
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.ReactorUtils;
import we.util.ThreadContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author hongqiaowei
 */

@Service
public class ApiConifg2appsService {

    private static final Logger log                       = LoggerFactory.getLogger(ApiConifg2appsService.class);

    private static final String fizzApiConfigAppSetSize   = "fizz_api_config_app_set_size";

    private static final String fizzApiConfigAppKeyPrefix = "fizz_api_config_app:";

    private static final String fizzApiConfigAppChannel   = "fizz_api_config_app_channel";

    private Map<Integer/* api config id */, Set<String/* app */>> apiConfig2appsMap = new HashMap<>(128);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
        this.init(this::lsnChannel);
    }

    public void refreshLocalCache() throws Throwable {
        this.init(null);
    }

    private void init(Runnable doAfterLoadCache) throws Throwable {
        Map<Integer, Set<String>> apiConfig2appsMapTmp = new HashMap<>(128);
        rt.opsForHash().entries(fizzApiConfigAppSetSize)
                .collectList()
                .map(
                    es -> {
                        log(es);
                        Mono initiateFlux = ReactorUtils.getInitiateMono();
                        for (Map.Entry<Object, Object> e : es) {
                            Integer apiConfigId = Integer.parseInt( (String) e.getKey()   );
                            int     appSetCount = Integer.parseInt( (String) e.getValue() );
                            for (int i = 0; i < appSetCount; i++) {
                                int iFinal = i;
                                initiateFlux = initiateFlux.flatMap(
                                    o -> {
                                        return
                                        rt.opsForSet().members(fizzApiConfigAppKeyPrefix + apiConfigId + '_' + iFinal)
                                                      .collectList()
                                                      .map(
                                                          as -> {
                                                              save(apiConfigId, as, apiConfig2appsMapTmp);
                                                              return ReactorUtils.NULL;
                                                          }
                                                      )
                                                      ;
                                    }
                                );
                            }
                        }
                        return initiateFlux;
                    }
                )
                .subscribe(
                    m -> {
                        m.subscribe(
                            e -> {
                                apiConfig2appsMap = apiConfig2appsMapTmp;
                                if (doAfterLoadCache != null) {
                                    doAfterLoadCache.run();
                                }
                            }
                        );
                    }
                );
    }

    private void log(List<Map.Entry<Object, Object>> es) {
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(fizzApiConfigAppSetSize).append('\n');
        for (Map.Entry<Object, Object> e : es) {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            b.append(key).append(":").append(value).append(' ');
        }
        log.info(b.toString());
    }

    private void save(Integer apiConfigId, List<String> as, Map<Integer, Set<String>> apiConfig2appsMap) {
        Set<String> appSet = apiConfig2appsMap.get(apiConfigId);
        if (appSet == null) {
            appSet = new HashSet<>();
            apiConfig2appsMap.put(apiConfigId, appSet);
        }
        appSet.addAll(as);
        log(apiConfigId, as);
    }

    private void log(Integer apiConfigId, List<String> apps) {
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(apiConfigId).append(" add: ");
        for (String a : apps) {
            b.append(a).append(' ');
        }
        log.info(b.toString());
    }

    private void lsnChannel() {
        rt.listenToChannel(fizzApiConfigAppChannel)
                .doOnError(
                        t -> {
                            log.error("lsn api config 2 apps channel", t);
                        }
                )
                .doOnComplete(
                        () -> {
                            log.info("success to lsn on api config 2 apps channel");
                        }
                )
                .doOnNext(
                    msg -> {
                        String json = msg.getMessage();
                        log.info("apiConfig2apps: " + json, LogService.BIZ_ID, "ac2as" + System.currentTimeMillis());
                        try {
                            ApiConfig2apps data = JacksonUtils.readValue(json, ApiConfig2apps.class);
                            updateApiConfig2appsMap(data);
                        } catch (Throwable t) {
                            log.error(Constants.Symbol.EMPTY, t);
                        }
                    }
                )
                .subscribe()
                ;
    }

    private void updateApiConfig2appsMap(ApiConfig2apps data) {
        Set<String> apps = apiConfig2appsMap.get(data.id);
        if (data.isDeleted == ApiConfig2apps.DELETED) {
            if (apps != null) {
                apps.removeAll(data.apps);
                log.info("remove " + data);
            }
        } else {
            if (apps == null) {
                apps = new HashSet<>(32);
                apiConfig2appsMap.put(data.id, apps);
            }
            apps.addAll(data.apps);
            log.info("add " + data);
        }
    }

    public boolean contains(int api, String app) {
        Set<String> apps = apiConfig2appsMap.get(api);
        if (apps == null) {
            return false;
        } else {
            return apps.contains(app);
        }
    }

    public Set<String> remove(int id) {
        return apiConfig2appsMap.remove(id);
    }

    public Map<Integer, Set<String>> getApiConfig2appsMap() {
        return apiConfig2appsMap;
    }
}
