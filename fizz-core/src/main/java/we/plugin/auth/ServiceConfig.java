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

    private String id;

    public Map<String/*gateway group*/,
                                        Map<Object/*method*/,
                                                              Map<String/*path patten*/, Set<ApiConfig>>
                                        >
           >
           apiConfigMap = new HashMap<>();

    public ServiceConfig(String id) {
        this.id = id;
    }

    public void add(ApiConfig ac) {
        for (String gatewayGroup : ac.gatewayGroups) {
            Map<Object, Map<String, Set<ApiConfig>>> method2pathPattenMap = apiConfigMap.computeIfAbsent(gatewayGroup, k -> new HashMap<>());
            Map<String, Set<ApiConfig>> pathPattern2apiConfigsMap = method2pathPattenMap.computeIfAbsent(ac.fizzMethod, k -> new HashMap<>());
            Set<ApiConfig> apiConfigs = pathPattern2apiConfigsMap.computeIfAbsent(ac.path, k -> new HashSet<>());
            apiConfigs.add(ac);
        }
        log.info("{} service add api config: {}", id, ac);
    }

    public void remove(ApiConfig ac) {
        for (String gatewayGroup : ac.gatewayGroups) {
            Map<Object, Map<String, Set<ApiConfig>>> method2pathPattenMap = apiConfigMap.get(gatewayGroup);
            if (method2pathPattenMap != null) {
                Map<String, Set<ApiConfig>> pathPattern2apiConfigsMap = method2pathPattenMap.get(ac.fizzMethod);
                if (pathPattern2apiConfigsMap != null) {
                    Set<ApiConfig> apiConfigs = pathPattern2apiConfigsMap.get(ac.path);
                    if (apiConfigs != null) {
                        apiConfigs.remove(ac);
                                                if (apiConfigs.isEmpty()) {
                                                    pathPattern2apiConfigsMap.remove(ac.path);
                                                    if (pathPattern2apiConfigsMap.isEmpty()) {
                                                        method2pathPattenMap.remove(ac.fizzMethod);
                                                        if (method2pathPattenMap.isEmpty()) {
                                                            apiConfigMap.remove(gatewayGroup);
                                                        }
                                                    }
                                                }
                    }
                }
            }
        }
        log.info("{} service remove api config: {}", id, ac);
    }

    public void update(ApiConfig ac) {
        for (String gatewayGroup : ac.gatewayGroups) {
            Map<Object, Map<String, Set<ApiConfig>>> method2pathPattenMap = apiConfigMap.computeIfAbsent(gatewayGroup, k -> new HashMap<>());
            Map<String, Set<ApiConfig>> pathPattern2apiConfigsMap = method2pathPattenMap.computeIfAbsent(ac.fizzMethod, k -> new HashMap<>());
            Set<ApiConfig> apiConfigs = pathPattern2apiConfigsMap.computeIfAbsent(ac.path, k -> new HashSet<>());
            apiConfigs.remove(ac);
            apiConfigs.add(ac);
        }
        log.info("{} service update api config: {}", id, ac);
    }

    @JsonIgnore
    public List<ApiConfig> getApiConfigs(boolean dedicatedLineRequest, Set<String> gatewayGroups, HttpMethod method, String path) {
        ArrayList<ApiConfig> result = ThreadContext.getArrayList(ThreadContext.arrayList0);
        for (String gatewayGroup : gatewayGroups) {
            List<ApiConfig> apiConfigs = getApiConfigs(dedicatedLineRequest, gatewayGroup, method, path);
            result.addAll(apiConfigs);
        }
        return result;
    }

    @JsonIgnore
    public List<ApiConfig> getApiConfigs(boolean dedicatedLineRequest, String gatewayGroup, HttpMethod method, String path) {
        Map<Object, Map<String, Set<ApiConfig>>> method2pathPattenMap = apiConfigMap.get(gatewayGroup);
        if (method2pathPattenMap == null) {
            return Collections.emptyList();
        } else {
            ArrayList<ApiConfig> result = ThreadContext.getArrayList();
            Map<String, Set<ApiConfig>> pathPattern2apiConfigsMap = method2pathPattenMap.get(method);
            if (pathPattern2apiConfigsMap != null) {
                checkPathPattern(pathPattern2apiConfigsMap, dedicatedLineRequest, path, result);
            }
            pathPattern2apiConfigsMap = method2pathPattenMap.get(ApiConfig.ALL_METHOD);
            if (pathPattern2apiConfigsMap != null) {
                checkPathPattern(pathPattern2apiConfigsMap, dedicatedLineRequest, path, result);
            }
            return result;
        }
    }

        private void checkPathPattern(Map<String, Set<ApiConfig>> pathPattern2apiConfigMap, boolean dedicatedLineRequest, String path, ArrayList<ApiConfig> result) {
            Set<Map.Entry<String, Set<ApiConfig>>> entries = pathPattern2apiConfigMap.entrySet();
            // boolean clear = false;
            for (Map.Entry<String, Set<ApiConfig>> entry : entries) {
                String pathPattern = entry.getKey();
                Set<ApiConfig> apiConfigs = entry.getValue();
                if (pathPattern.equals(path)) {
                    for (ApiConfig ac : apiConfigs) {
                        if (ac.allowAccess) {
                            /*if (!clear && !result.isEmpty()) {
                                result.clear();
                                clear = true;
                            }*/
                            if (dedicatedLineRequest) {
                                if (ac.dedicatedLine) {
                                    result.add(ac);
                                }
                            } else {
                                if (!ac.dedicatedLine) {
                                    result.add(ac);
                                }
                            }
                        }
                    }
                    /*if (clear && !result.isEmpty()) {
                        return;
                    }*/
                } else if (UrlTransformUtils.ANT_PATH_MATCHER.match(pathPattern, path)) {
                    for (ApiConfig ac : apiConfigs) {
                        if (ac.allowAccess) {
                            if (dedicatedLineRequest) {
                                if (ac.dedicatedLine) {
                                    result.add(ac);
                                }
                            } else {
                                if (!ac.dedicatedLine) {
                                    result.add(ac);
                                }
                            }
                        }
                    }
                }
            } // end for
        }
}
