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

package com.fizzgate.controller;

import com.fizzgate.controller.req.BaseManagerConfigReq;
import com.fizzgate.controller.req.GetApiConfigDetailReq;
import com.fizzgate.controller.req.GetApiConfigReq;
import com.fizzgate.controller.req.GetConfigReq;
import com.fizzgate.controller.req.GetConfigStrReq;
import com.fizzgate.controller.resp.ApiConfigInfo;
import com.fizzgate.controller.resp.ConfigResp;
import com.fizzgate.controller.resp.ConfigStrResp;
import com.fizzgate.controller.resp.GetApiConfigDetailResp;
import com.fizzgate.controller.resp.GetApiConfigResp;
import com.fizzgate.fizz.ConfigLoader;
import com.fizzgate.plugin.PluginConfig;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.auth.ApiConfig2appsService;
import com.fizzgate.plugin.auth.ApiConfigService;
import com.fizzgate.plugin.auth.GatewayGroup;
import com.fizzgate.plugin.auth.GatewayGroupService;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理后台配置控制器
 * Fizz管理后台通过该控制器暴露的接口获取聚合配置相关信息
 * @author zhongjie
 */
@RefreshScope
@RestController
@RequestMapping(value = "/admin/managerConfig")
public class ManagerConfigController {
    /**
     * 路由管理的路由类型集合
     */
    public static final Set<Byte> API_AUTH_PROXY_MODE_SET = Sets.newHashSet(ApiConfig.Type.SERVICE_AGGREGATE,
            ApiConfig.Type.SERVICE_DISCOVERY, ApiConfig.Type.REVERSE_PROXY, ApiConfig.Type.DUBBO);

    @Value("${fizz.manager.config.key:fizz-manager-key}")
    private String key;

    @Resource
    private ConfigLoader configLoader;

    @Resource
    private ApiConfigService apiConfigService;

    @Resource
    private GatewayGroupService gatewayGroupService;

    @Resource
    private ApiConfig2appsService apiConfig2appsService;

    @PostMapping("/getAggregateConfigs")
    public Mono<ConfigResp> getAggregateConfigs(@Valid @RequestBody GetConfigReq getConfigReq) {
        this.checkSign(getConfigReq);
        String serviceNameCondition = getConfigReq.getServiceName();
        String pathCondition = getConfigReq.getPath();
        String nameCondition = getConfigReq.getName();
        List<ConfigLoader.ConfigInfo> allConfigInfoList = configLoader.getConfigInfo();
        List<ConfigLoader.ConfigInfo> configInfoList = allConfigInfoList.stream().filter(it -> {
            if (StringUtils.hasText(serviceNameCondition)) {
                if (!it.getServiceName().contains(serviceNameCondition)) {
                    return Boolean.FALSE;
                }
            }
            if (StringUtils.hasText(pathCondition)) {
                if (!it.getPath().contains(pathCondition)) {
                    return Boolean.FALSE;
                }
            }
            if (StringUtils.hasText(nameCondition)) {
                if (!it.getConfigName().contains(nameCondition)) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }).collect(Collectors.toList());

        ConfigResp configResp = new ConfigResp();
        configResp.setConfigInfos(configInfoList);
        return Mono.just(configResp);
    }

    @PostMapping("/getConfigStr")
    public Mono<ConfigStrResp> getConfigStr(@Valid @RequestBody GetConfigStrReq getConfigStrReq) {
        this.checkSign(getConfigStrReq);
        String configId = getConfigStrReq.getConfigId();
        String configStr = configLoader.getConfigStr(configId);
        ConfigStrResp configStrResp = new ConfigStrResp();
        configStrResp.setConfigStr(configStr);
        return Mono.just(configStrResp);
    }

    @PostMapping("/getApiConfigs")
    public Mono<GetApiConfigResp> getApiConfigs(@Valid @RequestBody GetApiConfigReq getApiConfigReq) {
        this.checkSign(getApiConfigReq);
        Integer current = getApiConfigReq.getCurrent();
        Integer size = getApiConfigReq.getSize();

        String gatewayGroup = getApiConfigReq.getGatewayGroup();
        String service = getApiConfigReq.getService();
        String path = getApiConfigReq.getPath();
        Set<String> methods = getApiConfigReq.getMethods();
        Set<String> plugins = getApiConfigReq.getPlugins();
        String access = getApiConfigReq.getAccess();

        Set<String> currentGatewayGroupSet = gatewayGroupService.currentGatewayGroupSet;

        List<ApiConfigInfo> apiConfigInfoList = apiConfigService.getApiConfigMap().values().stream().filter(it -> {
            if (!currentGatewayGroupSet.contains(it.firstGatewayGroup)) {
                return false;
            }

            if (!API_AUTH_PROXY_MODE_SET.contains(it.type)) {
                return false;
            }

            if (StringUtils.hasText(gatewayGroup)) {
                if (!it.firstGatewayGroup.equals(gatewayGroup)) {
                    return false;
                }
            }

            if (StringUtils.hasText(service)) {
                if (!it.service.contains(service)) {
                    return false;
                }
            }

            if (StringUtils.hasText(path)) {
                if (!it.path.contains(path)) {
                    return false;
                }
            }

            if (!CollectionUtils.isEmpty(methods)) {
                if (!methods.contains(it.fizzMethod instanceof HttpMethod ? ((HttpMethod) it.fizzMethod).name() : it.fizzMethod)) {
                    return false;
                }
            }

            if (!CollectionUtils.isEmpty(plugins)) {
                boolean match = false;
                for (PluginConfig pluginConfig : it.pluginConfigs) {
                    if (plugins.contains(pluginConfig.plugin)) {
                        match = true;
                        break;
                    }
                }

                if (!match) {
                    GatewayGroup group = gatewayGroupService.get(it.firstGatewayGroup);
                    if (group != null) {
                        for (PluginConfig pluginConfig : group.pluginConfigs) {
                            if (plugins.contains(pluginConfig.plugin)) {
                                match = true;
                                break;
                            }
                        }
                    }

                    if (!match) {
                        return false;
                    }
                }
            }

            if ("a".equals(access) && !it.allowAccess) {
                return false;
            }

            if ("f".equals(access) && it.allowAccess) {
                return false;
            }

            return true;
        }).map(it -> {
            ApiConfigInfo apiConfigInfo = new ApiConfigInfo();
            apiConfigInfo.setId((long) it.id);
            apiConfigInfo.setGatewayGroup(it.firstGatewayGroup);
            apiConfigInfo.setProxyMode(it.type);
            apiConfigInfo.setIsDedicatedLine(it.dedicatedLine ? (byte) 1 : 0);
            apiConfigInfo.setService(it.service);
            apiConfigInfo.setPath(it.path);
            apiConfigInfo.setMethod(it.fizzMethod instanceof HttpMethod ? ((HttpMethod) it.fizzMethod).name() : "");

            List<PluginConfig> gatewayGroupPluginConfigs = null;
            GatewayGroup group = gatewayGroupService.get(it.firstGatewayGroup);
            if (group != null) {
                gatewayGroupPluginConfigs = group.pluginConfigs;
            }
            if (CollectionUtils.isEmpty(gatewayGroupPluginConfigs)) {
                apiConfigInfo.setPlugins(it.pluginConfigs.stream().map(pluginConfig -> pluginConfig.plugin).collect(Collectors.toList()));
            } else {
                List<PluginConfig> pcs = new ArrayList<>(gatewayGroupPluginConfigs.size() + it.pluginConfigs.size());
                pcs.addAll(gatewayGroupPluginConfigs);
                pcs.addAll(it.pluginConfigs);
                pcs.sort(null);
                apiConfigInfo.setPlugins(pcs.stream().map(pluginConfig -> pluginConfig.plugin).collect(Collectors.toList()));
            }

            apiConfigInfo.setAccess(it.allowAccess ? "a" : "f");
            return apiConfigInfo;
        }).collect(Collectors.toList());

        GetApiConfigResp getApiConfigResp = new GetApiConfigResp();
        getApiConfigResp.setTotal((long) apiConfigInfoList.size());

        apiConfigInfoList.sort(Comparator.comparing(ApiConfigInfo::getId).reversed());

        int apiConfigListSize = apiConfigInfoList.size();
        int startIndex = (current - 1) * size;
        if (startIndex >= apiConfigListSize) {
            return Mono.just(getApiConfigResp);
        }

        int endIndex = current * size;
        if (endIndex > apiConfigListSize) {
            endIndex = apiConfigListSize;
        }
        getApiConfigResp.setApiConfigInfos(apiConfigInfoList.subList(startIndex, endIndex));
        return Mono.just(getApiConfigResp);
    }


    @PostMapping("/getApiConfigDetail")
    public Mono<GetApiConfigDetailResp> getApiConfigDetail(@Valid @RequestBody GetApiConfigDetailReq getApiConfigDetailReq) {
        this.checkSign(getApiConfigDetailReq);
        Long apiConfigId = getApiConfigDetailReq.getApiConfigId();

        ApiConfig apiConfig = apiConfigService.getApiConfigMap().get(apiConfigId.intValue());

        GetApiConfigDetailResp getApiConfigDetailResp = new GetApiConfigDetailResp();
        if (apiConfig == null) {
            getApiConfigDetailResp.setExist(false);
        } else {
            getApiConfigDetailResp.setExist(true);
            getApiConfigDetailResp.setId((long) apiConfig.id);
            getApiConfigDetailResp.setGatewayGroup(apiConfig.firstGatewayGroup);
            getApiConfigDetailResp.setService(apiConfig.service);
            getApiConfigDetailResp.setMethod(apiConfig.fizzMethod instanceof HttpMethod ? ((HttpMethod) apiConfig.fizzMethod).name() : "");
            getApiConfigDetailResp.setPath(apiConfig.path);
            getApiConfigDetailResp.setAppEnabled(apiConfig.checkApp);
            if (apiConfig.checkApp) {
                getApiConfigDetailResp.setApps(apiConfig2appsService.getApiConfig2appsMap().get(apiConfig.id));
            }
            getApiConfigDetailResp.setAccess(apiConfig.allowAccess ? "a" : "f");
            getApiConfigDetailResp.setProxyMode(apiConfig.type);
            getApiConfigDetailResp.setBackendService(apiConfig.backendService);
            getApiConfigDetailResp.setBackendPath(apiConfig.backendPath);

            getApiConfigDetailResp.setApiPlugins(apiConfig.pluginConfigs.stream().map(pluginConfig -> {
                GetApiConfigDetailResp.ApiPluginVO apiPluginVO = new GetApiConfigDetailResp.ApiPluginVO();
                apiPluginVO.setPlugin(pluginConfig.plugin);
                apiPluginVO.setConfig(pluginConfig.config);
                apiPluginVO.setOrder(pluginConfig.order);
                return apiPluginVO;
            }).collect(Collectors.toList()));

            getApiConfigDetailResp.setApiBackends(apiConfig.httpHostPorts);
            getApiConfigDetailResp.setRpcMethod(apiConfig.rpcMethod);
            getApiConfigDetailResp.setRpcParamTypes(apiConfig.rpcParamTypes);
            getApiConfigDetailResp.setRpcVersion(apiConfig.rpcVersion);
            getApiConfigDetailResp.setRpcGroup(apiConfig.rpcGroup);
            getApiConfigDetailResp.setTimeout((int) apiConfig.timeout);
            getApiConfigDetailResp.setRetryCount(apiConfig.retryCount);
            getApiConfigDetailResp.setRetryInterval(apiConfig.retryInterval);

            getApiConfigDetailResp.setIsDedicatedLine(apiConfig.dedicatedLine ? (byte) 1 : 0);

            getApiConfigDetailResp.setRegistryName(apiConfig.registryCenter);
        }

        return Mono.just(getApiConfigDetailResp);
    }

    private void checkSign(BaseManagerConfigReq req) {
        if (!req.checkSign(key)) {
            throw new RuntimeException("验证签名失败");
        }
    }
}
