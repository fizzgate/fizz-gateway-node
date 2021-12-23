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

package we.service_registry.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.util.StringUtils;
import we.service_registry.FizzServiceRegistration;
import we.service_registry.RegistryCenter;
import we.util.Consts;
import we.util.Utils;

import java.util.Collections;
import java.util.List;

/**
 * @author hongqiaowei
 */

public class FizzNacosServiceRegistration extends FizzServiceRegistration {

    private NamingService namingService;

    private final String  groupName;

    private List<String>  clusterNameList;

    private boolean       useGroupName;

    private boolean       userClusterName;

    public FizzNacosServiceRegistration(String id, NacosRegistration registration, NacosServiceRegistry serviceRegistry, NamingService namingService) {
        super(id, registration, serviceRegistry);
        this.namingService = namingService;
        NacosDiscoveryProperties discoveryProperties = registration.getNacosDiscoveryProperties();
        groupName = discoveryProperties.getGroup();
        if (StringUtils.hasText(groupName)) {
            useGroupName = true;
        }
        String clusterName = discoveryProperties.getClusterName();
        if (StringUtils.hasText(clusterName)) {
            userClusterName = true;
            clusterNameList = Collections.singletonList(clusterName);
        }
    }

    public NamingService getNamingService() {
        return namingService;
    }

    @Override
    public RegistryCenter.Status getRegistryCenterStatus() {
        String status = namingService.getServerStatus();
        return transfrom(status);
    }

    private RegistryCenter.Status transfrom(String status) {
        if (status.equals("UP")) {
            return RegistryCenter.Status.UP;

        } else if (status.equals("DOWN")) {
            return RegistryCenter.Status.DOWN;

        } else {
            log.warn("{} status is {}", id, status);
            return RegistryCenter.Status.UNKNOWN;
        }
    }

    @Override
    public String getInstance(String service) {
        Instance instance = getInstanceInfo(service);
        return instance.getIp() + Consts.S.COLON + instance.getPort();
    }

    public Instance getInstanceInfo(String service) {
        Instance instance = null;
        try {
            if (useGroupName && userClusterName) {
                instance = namingService.selectOneHealthyInstance(service, groupName, clusterNameList);
            } else if (useGroupName) {
                instance = namingService.selectOneHealthyInstance(service, groupName);
            } else if (userClusterName) {
                instance = namingService.selectOneHealthyInstance(service, clusterNameList);
            } else {
                instance = namingService.selectOneHealthyInstance(service);
            }
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        if (instance == null) {
            throw Utils.runtimeExceptionWithoutStack(id + " nacos no " + service);
        }
        return instance;
    }
}
