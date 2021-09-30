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

    private String id;

//  @JsonIgnore
//  public Map<Integer, ApiConfig> apiConfigMap = new HashMap<>();

//  public Map<String, Map<Object, GatewayGroup2apiConfig>> path2methodToApiConfigMapMap = new HashMap<>();

    public Map<String/*gateway group*/,
                                        Map<Object/*method*/,
                                                              Map<String/*path patten*/, ApiConfig>
                                        >
           >
           apiConfigMap = new HashMap<>();

    public ServiceConfig(String id) {
        this.id = id;
    }

    public void add(ApiConfig ac) {
//      apiConfigMap.put(ac.id, ac);
        /*
        Map<Object, GatewayGroup2apiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            method2apiConfigMap = new HashMap<Object, GatewayGroup2apiConfig>();
            GatewayGroup2apiConfig gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
            gatewayGroup2apiConfig.add(ac);
            method2apiConfigMap.put(ac.fizzMethod, gatewayGroup2apiConfig);
            path2methodToApiConfigMapMap.put(ac.path, method2apiConfigMap);
        } else {
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2apiConfigMap.get(ac.fizzMethod);
            if (gatewayGroup2apiConfig == null) {
                gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
                method2apiConfigMap.put(ac.fizzMethod, gatewayGroup2apiConfig);
            }
            gatewayGroup2apiConfig.add(ac);
        }
        log.info("add " + ac);
        */

        for (String gatewayGroup : ac.gatewayGroups) {
            Map<Object, Map<String, ApiConfig>> method2pathPattenMap = apiConfigMap.get(gatewayGroup);
            if (method2pathPattenMap == null) {
                method2pathPattenMap = new HashMap<>();
                apiConfigMap.put(gatewayGroup, method2pathPattenMap);
            }
            Map<String, ApiConfig> pathPattern2apiConfigMap = method2pathPattenMap.get(ac.fizzMethod);
            if (pathPattern2apiConfigMap == null) {
                pathPattern2apiConfigMap = new HashMap<>();
                method2pathPattenMap.put(ac.fizzMethod, pathPattern2apiConfigMap);
            }
            pathPattern2apiConfigMap.put(ac.path, ac);
        }
        log.info("add api config: {}", ac);
    }

    public void remove(ApiConfig ac) {
//      ApiConfig remove = apiConfigMap.remove(ac.id);
        /*
        Map<Object, GatewayGroup2apiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            log.info("no config to delete for " + ac.service + ' ' + ac.path);
        } else {
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2apiConfigMap.get(ac.fizzMethod);
            if (gatewayGroup2apiConfig == null) {
                log.info("no config to delete for " + ac.service + ' ' + ac.fizzMethod + ' ' + ac.path);
            } else {
                log.info(id + " remove " + ac);
                gatewayGroup2apiConfig.remove(ac);
                if (gatewayGroup2apiConfig.getConfigMap().isEmpty()) {
                    method2apiConfigMap.remove(ac.fizzMethod);
                    if (method2apiConfigMap.isEmpty()) {
                        path2methodToApiConfigMapMap.remove(ac.path);
                    }
                }
            }
        }
        */

        for (String gatewayGroup : ac.gatewayGroups) {
            Map<Object, Map<String, ApiConfig>> method2pathPattenMap = apiConfigMap.get(gatewayGroup);
            if (method2pathPattenMap != null) {
                Map<String, ApiConfig> pathPattern2apiConfigMap = method2pathPattenMap.get(ac.fizzMethod);
                if (pathPattern2apiConfigMap != null) {
                    pathPattern2apiConfigMap.remove(ac.path);

                    if (pathPattern2apiConfigMap.isEmpty()) {
                        method2pathPattenMap.remove(ac.fizzMethod);
                        if (method2pathPattenMap.isEmpty()) {
                            apiConfigMap.remove(gatewayGroup);
                        }
                    }
                }
            }
        }
        log.info("remove api config: {}", ac);
    }

    public void update(ApiConfig ac) {
//      ApiConfig prev = apiConfigMap.put(ac.id, ac);
//      log.info(prev + " is updated by " + ac + " in api config map");
        Map<Object, GatewayGroup2apiConfig> method2apiConfigMap = path2methodToApiConfigMapMap.get(ac.path);
        if (method2apiConfigMap == null) {
            method2apiConfigMap = new HashMap<Object, GatewayGroup2apiConfig>();
            GatewayGroup2apiConfig gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
            gatewayGroup2apiConfig.add(ac);
            method2apiConfigMap.put(ac.fizzMethod, gatewayGroup2apiConfig);
            path2methodToApiConfigMapMap.put(ac.path, method2apiConfigMap);
        } else {
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2apiConfigMap.get(ac.fizzMethod);
            if (gatewayGroup2apiConfig == null) {
                gatewayGroup2apiConfig = new GatewayGroup2apiConfig();
                method2apiConfigMap.put(ac.fizzMethod, gatewayGroup2apiConfig);
                gatewayGroup2apiConfig.add(ac);
            } else {
                log.info(id + " update " + ac);
                gatewayGroup2apiConfig.update(ac);
            }
        }
    }

    @JsonIgnore
    public List<ApiConfig> getApiConfigs(HttpMethod method, String path, String gatewayGroup) {

        List<GatewayGroup2apiConfig> matchGatewayGroup2apiConfigs = ThreadContext.getArrayList(gg2acs);

        Set<Map.Entry<String, Map<Object, GatewayGroup2apiConfig>>> es = path2methodToApiConfigMapMap.entrySet();
        for (Map.Entry<String, Map<Object, GatewayGroup2apiConfig>> e : es) {
            Map<Object, GatewayGroup2apiConfig> method2gatewayGroupToApiConfigMap = e.getValue();
            GatewayGroup2apiConfig gatewayGroup2apiConfig = method2gatewayGroupToApiConfigMap.get(method);
            if (gatewayGroup2apiConfig == null) {
                gatewayGroup2apiConfig = method2gatewayGroupToApiConfigMap.get(ApiConfig.ALL_METHOD);
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
            ThreadContext.set(ApiConfigService.AUTH_MSG, id + " no route match " + method + ' ' + path);
            return Collections.emptyList();
        } else {
            List<ApiConfig> lst = ThreadContext.getArrayList(acs);
            for (int i = 0; i < matchGatewayGroup2apiConfigs.size(); i++) {
                GatewayGroup2apiConfig gatewayGroup2apiConfig = matchGatewayGroup2apiConfigs.get(i);
                Set<ApiConfig> apiConfigs = gatewayGroup2apiConfig.get(gatewayGroup);
                if (apiConfigs == null) {
                    ThreadContext.set(ApiConfigService.AUTH_MSG, "route which match " + id + ' ' + method + ' ' + path + " is not exposed to " + gatewayGroup);
                } else {
                    for (ApiConfig ac : apiConfigs) {
                        if (ac.access == ApiConfig.ALLOW) {
                            lst.add(ac);
                        }
                    }
                    if (lst.isEmpty()) {
                        ThreadContext.set(ApiConfigService.AUTH_MSG, "route which match " + id + ' ' + method + ' ' + path + " not allow access");
                    }
                }
            }
            return lst;
        }
    }
}
