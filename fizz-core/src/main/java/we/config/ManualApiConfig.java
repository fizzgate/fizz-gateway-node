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

package we.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import we.plugin.auth.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @apiNote tool for test only
 * <p/>
 *
 * @author hongqiaowei
 */

public abstract class ManualApiConfig {

    protected static final Logger log = LoggerFactory.getLogger(ManualApiConfig.class);

    @Resource
    private GatewayGroupService gatewayGroupService;

    @Resource
    private ApiConfigService apiConfigService;

    public abstract List<ApiConfig> setApiConfigs();

    @PostConstruct
    public void iniApiConfigs() {
        // gatewayGroupService.currentGatewayGroupSet = Stream.of(GatewayGroup.DEFAULT).collect(Collectors.toSet());
        gatewayGroupService.currentGatewayGroupSet.add(GatewayGroup.DEFAULT);
        List<ApiConfig> apiConfigs = setApiConfigs();
        for (ApiConfig ac : apiConfigs) {
            ServiceConfig sc = apiConfigService.serviceConfigMap.get(ac.service);
            if (sc == null) {
                sc = new ServiceConfig(ac.service);
                apiConfigService.serviceConfigMap.put(ac.service, sc);
            }
            sc.add(ac);
            log.info("manual add {}", ac);
        }
    }
}
