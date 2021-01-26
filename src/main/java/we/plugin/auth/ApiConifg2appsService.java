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
import we.config.AggregateRedisConfig;
import we.flume.clients.log4j2appender.LogService;
import we.util.Constants;
import we.util.JacksonUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author hongqiaowei
 */

@Service
public class ApiConifg2appsService {

    private static final Logger log                     = LoggerFactory.getLogger(ApiConifg2appsService.class);

    private static final String fizzApiConfigAppChannel = "fizz_api_config_app_channel";

    private Map<Integer/* api config id */, Set<String/* app */>> apiConfig2appsMap = new HashMap<>(128);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @PostConstruct
    public void init() throws Throwable {
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
