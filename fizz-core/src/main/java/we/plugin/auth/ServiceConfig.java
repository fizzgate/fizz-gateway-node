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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import we.util.ThreadContext;
import we.util.UrlTransformUtils;

import java.util.*;

/**
 * @author hongqiaowei
 */

public class ServiceConfig {

    private static final Logger log    = LoggerFactory.getLogger(ServiceConfig.class);

    private static final String gg2acs = "$gg2acs";

    private static final String acs    = "$acs";

    public String id;

    @JsonIgnore
    public Map<Integer, ApiConfig> apiConfigMap = new HashMap<>(32);

    public Map<String, EnumMap<HttpMethod, GatewayGroup2apiConfig>> path2methodToApiConfigMapMap = new HashMap<>(6);

    public ServiceConfig(String id) {
        this.id = id;
    }

    public void add(ApiConfig ac) {
        apiConfigMap.put(ac.id, ac);
        EnumMap<HttpMethod, GatewayGroup2apiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            method2apiConfigMap = new EnumMap<>(HttpMethod.class);
            GatewayGroup2apiConfig gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
            gatewayGroup2apiConfig.add(ac);
            method2apiConfigMap.put(ac.method, gatewayGroup2apiConfig);
            path2methodToApiConfigMapMap.put(ac.path, method2apiConfigMap);
        } else {
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2apiConfigMap.get(ac.method);
            if (gatewayGroup2apiConfig == null) {
                gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
                method2apiConfigMap.put(ac.method, gatewayGroup2apiConfig);
            }
            gatewayGroup2apiConfig.add(ac);
        }
        log.info("add " + ac);
    }

    public void remove(ApiConfig ac) {
        ApiConfig remove = apiConfigMap.remove(ac.id);
        Map<HttpMethod, GatewayGroup2apiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            log.info("no config to delete for " + ac.service + ' ' + ac.path);
        } else {
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2apiConfigMap.get(ac.method);
            if (gatewayGroup2apiConfig == null) {
                log.info("no config to delete for " + ac.service + ' ' + ac.method + ' ' + ac.path);
            } else {
                log.info(id + " remove " + ac);
                gatewayGroup2apiConfig.remove(ac);
                if (gatewayGroup2apiConfig.getConfigMap().isEmpty()) {
                    method2apiConfigMap.remove(ac.method);
                    if (method2apiConfigMap.isEmpty()) {
                        path2methodToApiConfigMapMap.remove(ac.path);
                    }
                }
            }
        }
    }

    public void update(ApiConfig ac) {
        ApiConfig prev = apiConfigMap.put(ac.id, ac);
        log.info(prev + " is updated by " + ac + " in api config map");
        EnumMap<HttpMethod, GatewayGroup2apiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            method2apiConfigMap = new EnumMap<>(HttpMethod.class);
            GatewayGroup2apiConfig gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
            gatewayGroup2apiConfig.add(ac);
            method2apiConfigMap.put(ac.method, gatewayGroup2apiConfig);
            path2methodToApiConfigMapMap.put(ac.path, method2apiConfigMap);
        } else {
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2apiConfigMap.get(ac.method);
            if (gatewayGroup2apiConfig == null) {
                gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
                method2apiConfigMap.put(ac.method, gatewayGroup2apiConfig);
                gatewayGroup2apiConfig.add(ac);
            } else {
                log.info(id + " update " + ac);
                gatewayGroup2apiConfig.update(ac);
            }
        }
    }

    @JsonIgnore
    public List<ApiConfig> getApiConfigs(HttpMethod method, String path, String gatewayGroup) {

        List<GatewayGroup2apiConfig> matchGatewayGroup2apiConfigs = ThreadContext.getArrayList(gg2acs, GatewayGroup2apiConfig.class);

        Set<Map.Entry<String, EnumMap<HttpMethod, GatewayGroup2apiConfig>>> es = path2methodToApiConfigMapMap.entrySet();
        for (Map.Entry<String, EnumMap<HttpMethod, GatewayGroup2apiConfig>> e : es) {
            EnumMap<HttpMethod, GatewayGroup2apiConfig> method2gatewayGroupToApiConfigMap = e.getValue();
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2gatewayGroupToApiConfigMap.get(method);
            if (gatewayGroup2apiConfig == null) {
                gatewayGroup2apiConfig = method2gatewayGroupToApiConfigMap.get(HttpMethod.TRACE);
            }
            if (gatewayGroup2apiConfig != null) {
                String pathPattern = e.getKey();
                if (ApiConfig.isAntPathPattern(pathPattern)) {
                    if (UrlTransformUtils.ANT_PATH_MATCHER.match(pathPattern, path)) {
                        matchGatewayGroup2apiConfigs.add(gatewayGroup2apiConfig);
                    }
                } else if (path.equals(pathPattern)) {
                    matchGatewayGroup2apiConfigs.add(gatewayGroup2apiConfig);
                }
            }
        }

        if (matchGatewayGroup2apiConfigs.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ApiConfig> lst = ThreadContext.getArrayList(acs, ApiConfig.class);
            for (GatewayGroup2apiConfig gatewayGroup2apiConfig : matchGatewayGroup2apiConfigs) {
                Set<ApiConfig> apiConfigs = gatewayGroup2apiConfig.get(gatewayGroup);
                if (apiConfigs != null) {
                    lst.addAll(apiConfigs);
                }
            }
            return lst;
        }
    }
}
