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

import com.fasterxml.jackson.annotation.JsonIgnore;
import we.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lancer
 */

public class ServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

    private static final String forward_slash_str = String.valueOf(Constants.Symbol.FORWARD_SLASH);

    public  String id;

    private Map<Integer, ApiConfig> apiConfigMap = new HashMap<>(32);

    private Map<String, EnumMap<HttpMethod, ApiConfig>> path2methodToApiConfigMapMap = new HashMap<>(32);

    public ServiceConfig(String id) {
        this.id = id;
    }

    public Map<Integer, ApiConfig> apiConfigMap() {
        return apiConfigMap;
    }

    public Map<String, EnumMap<HttpMethod, ApiConfig>> getPath2methodToApiConfigMapMap() {
        return path2methodToApiConfigMapMap;
    }

    public void remove(ApiConfig ac) {
        ApiConfig remove = apiConfigMap.remove(ac.id);
        log.info(remove + " is removed from api config map");
        Map<HttpMethod, ApiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap != null) {
            ApiConfig r = method2apiConfigMap.remove(ac.method);
            log.info(r + " is removed from method 2 api config map");
            if (method2apiConfigMap.isEmpty()) {
                path2methodToApiConfigMapMap.remove(ac.path);
            }
        } else {
            log.info("no method 2 api config map for " + ac.path);
        }
    }

    public void add(ApiConfig ac) {
        apiConfigMap.put(ac.id, ac);
        EnumMap<HttpMethod, ApiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            method2apiConfigMap = new EnumMap<>(HttpMethod.class);
            path2methodToApiConfigMapMap.put(ac.path, method2apiConfigMap);
        }
        method2apiConfigMap.put(ac.method, ac);
        log.info(ac + " is added to api config map");
    }

    public void update(ApiConfig ac) {
        ApiConfig prev = apiConfigMap.put(ac.id, ac);
        log.info(prev + " is updated by " + ac + " in api config map");
        EnumMap<HttpMethod, ApiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            method2apiConfigMap = new EnumMap<>(HttpMethod.class);
            path2methodToApiConfigMapMap.put(ac.path, method2apiConfigMap);
        }
        ApiConfig put = method2apiConfigMap.put(ac.method, ac);
        log.info(put + " is updated by " + ac + " in method 2 api config map");
    }

    @JsonIgnore
    public ApiConfig getApiConfig(HttpMethod method, String path) {
        while (true) {
            EnumMap<HttpMethod, ApiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(path);
            if (method2apiConfigMap == null) {
                int i = path.lastIndexOf(Constants.Symbol.FORWARD_SLASH);
                if (i == 0) {
                    method2apiConfigMap = path2methodToApiConfigMapMap.get(forward_slash_str);
                    if (method2apiConfigMap == null) {
                        return null;
                    } else {
                        return getApiConfig0(method, method2apiConfigMap);
                    }
                } else {
                    path = path.substring(0, i);
                }
            } else {
                return getApiConfig0(method, method2apiConfigMap);
            }
        }
    }

    private ApiConfig getApiConfig0(HttpMethod method, EnumMap<HttpMethod, ApiConfig> method2apiConfigMap) {
        ApiConfig ac = method2apiConfigMap.get(method);
        if (ac == null) {
            return method2apiConfigMap.get(HttpMethod.X);
        } else {
            return ac;
        }
    }
}
