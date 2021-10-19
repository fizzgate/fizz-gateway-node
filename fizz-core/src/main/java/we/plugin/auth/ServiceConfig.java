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

    private static final Logger log   = LoggerFactory.getLogger(ServiceConfig.class);

//  private static final String gmpT  = "gmpT";

//  private static final String gsmpT = "gsmpT";

    private String id;

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
        log.info("{} service add api config: {}", id, ac);
    }

    public void remove(ApiConfig ac) {
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
        log.info("{} service remove api config: {}", id, ac);
    }

    public void update(ApiConfig ac) {
        ApiConfig prevApiConfig = null;
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
            prevApiConfig = pathPattern2apiConfigMap.put(ac.path, ac);
        }
        log.info("{} service update api config {} with {}", id, prevApiConfig, ac);
    }

    @JsonIgnore
    public List<ApiConfig> getApiConfigs(Set<String> gatewayGroups, HttpMethod method, String path) {
        ArrayList<ApiConfig> result = ThreadContext.getArrayList(ThreadContext.arrayList0);
        for (String gatewayGroup : gatewayGroups) {
            List<ApiConfig> apiConfigs = getApiConfigs(gatewayGroup, method, path);
            result.addAll(apiConfigs);
        }
        return result;
    }

    @JsonIgnore
    public List<ApiConfig> getApiConfigs(String gatewayGroup, HttpMethod method, String path) {
        Map<Object, Map<String, ApiConfig>> method2pathPattenMap = apiConfigMap.get(gatewayGroup);
        if (method2pathPattenMap == null) {
            return Collections.emptyList();
        } else {
//          ArrayList<ApiConfig> result = ThreadContext.getArrayList(gmpT);
            ArrayList<ApiConfig> result = ThreadContext.getArrayList();
            Map<String, ApiConfig> pathPattern2apiConfigMap = method2pathPattenMap.get(method);
            if (pathPattern2apiConfigMap != null) {
                checkPathPattern(pathPattern2apiConfigMap, path, result);
            }
            pathPattern2apiConfigMap = method2pathPattenMap.get(ApiConfig.ALL_METHOD);
            if (pathPattern2apiConfigMap != null) {
                checkPathPattern(pathPattern2apiConfigMap, path, result);
            }
            return result;
        }
    }

    private void checkPathPattern(Map<String, ApiConfig> pathPattern2apiConfigMap, String path, ArrayList<ApiConfig> result) {
        Set<Map.Entry<String, ApiConfig>> entries = pathPattern2apiConfigMap.entrySet();
        for (Map.Entry<String, ApiConfig> entry : entries) {
            String pathPattern  = entry.getKey();
            ApiConfig apiConfig = entry.getValue();
            if (apiConfig.access == ApiConfig.ALLOW) {
                if (apiConfig.exactMatch) {
                    if (pathPattern.equals(path)) {
                        result.add(apiConfig);
                    }
                } else {
                    if (UrlTransformUtils.ANT_PATH_MATCHER.match(pathPattern, path)) {
                        result.add(apiConfig);
                    }
                }
            }
        }
    }
}
