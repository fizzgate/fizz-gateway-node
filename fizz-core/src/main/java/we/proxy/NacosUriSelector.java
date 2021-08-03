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

package we.proxy;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * The Nacos implementation of {@code DiscoveryClientUriSelector}
 *
 * @author zhongjie
 */
@ConditionalOnProperty(value = "spring.cloud.nacos.discovery.enabled")
@Service
public class NacosUriSelector extends AbstractDiscoveryClientUriSelector {
    private static final Logger log = LoggerFactory.getLogger(NacosUriSelector.class);

    public NacosUriSelector(NacosServiceManager nacosServiceManager, NacosDiscoveryProperties discoveryProperties) {
        this.nacosServiceManager = nacosServiceManager;
        this.discoveryProperties = discoveryProperties;
    }

    final private NacosServiceManager nacosServiceManager;
    private NamingService naming;
    final private NacosDiscoveryProperties discoveryProperties;
    private String groupName;
    private List<String> clusterNameList;
    private boolean useGroupName;
    private boolean userClusterName;

    @PostConstruct
    public void init() {
        naming = nacosServiceManager.getNamingService(discoveryProperties.getNacosProperties());
        this.groupName = discoveryProperties.getGroup();
        if (StringUtils.hasText(groupName)) {
            this.useGroupName = true;
        }
        String clusterName = discoveryProperties.getClusterName();
        if (StringUtils.hasText(clusterName)) {
            this.userClusterName = true;
            this.clusterNameList = Collections.singletonList(clusterName);
        }
    }

    @Override
    public String getNextUri(String service, String relativeUri) {
        Instance instance = this.selectOneHealthyInstance(service);
        return super.buildUri(instance.getIp(), instance.getPort(), relativeUri);
    }

    @Override
    public ServiceInstance getNextInstance(String service) {
        Instance inst = this.selectOneHealthyInstance(service);
        return new ServiceInstance(inst.getIp(), inst.getPort());
    }

    private Instance selectOneHealthyInstance(String service) {
        Instance instance = null;
        try {
            if (useGroupName && userClusterName) {
                instance = naming.selectOneHealthyInstance(service, groupName, clusterNameList);
            } else if (useGroupName) {
                instance = naming.selectOneHealthyInstance(service, groupName);
            } else if (userClusterName) {
                instance = naming.selectOneHealthyInstance(service, clusterNameList);
            } else {
                instance = naming.selectOneHealthyInstance(service);
            }
        } catch (NacosException e) {
            log.warn("Nacos selectOneHealthyInstance({}) exception", service, e);
        }

        if (instance == null) {
            throw new RuntimeException("Nacos no " + service, null, false, false) {
            };
        }

        return instance;
    }
}
