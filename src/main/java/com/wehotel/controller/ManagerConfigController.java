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

package com.wehotel.controller;

import com.wehotel.controller.req.BaseManagerConfigReq;
import com.wehotel.controller.req.GetConfigStrReq;
import com.wehotel.controller.req.GetConfigReq;
import com.wehotel.controller.resp.ConfigStrResp;
import com.wehotel.controller.resp.ConfigResp;
import com.wehotel.fizz.ConfigLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台配置控制器
 * Fizz管理后台通过该控制器暴露的接口获取聚合配置相关信息
 * @author zhongjie
 */
@RestController
@RequestMapping(value = "/managerConfig")
public class ManagerConfigController {
    @Value("${fizz.manager.config.key:fizz-manager-key}")
    private String key;

    @Resource
    private ConfigLoader configLoader;

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

    private void checkSign(BaseManagerConfigReq req) {
        if (!req.checkSign(key)) {
            throw new RuntimeException("验证签名失败");
        }
    }
}
