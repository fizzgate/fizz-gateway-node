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

package we.service_registry.eureka;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author hongqiaowei
 */

public class FizzEurekaProperties {

    public ConfigurableApplicationContext applicationContext;

    private String id;

    public String appName;

    private String virtualHostName;

    public String ipAddress;

    public int nonSecurePort = 80;

    private String instanceId;

    public boolean preferIpAddress = true;

    public boolean securePortEnabled = false;

    private String healthCheckUrl;

    public DataCenterInfo dataCenterInfo = new MyDataCenterInfo(
            DataCenterInfo.Name.MyOwn);

    public String region = "default";

    public String zone = EurekaClientConfigBean.DEFAULT_ZONE;

    public String serviceUrl;

    public int securePort = 443;

    public FizzEurekaProperties applicationContext(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }

    public FizzEurekaProperties id(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        if (id == null) {
            id = appName + ':' + serviceUrl;
        }
        return id;
    }

    public FizzEurekaProperties appName(String appName) {
        this.appName = appName;
        return this;
    }

    public FizzEurekaProperties virtualHostName(String virtualHostName) {
        this.virtualHostName = virtualHostName;
        return this;
    }

    public String getVirtualHostName() {
        if (virtualHostName == null) {
            virtualHostName = appName;
        }
        return virtualHostName;
    }

    public FizzEurekaProperties ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public FizzEurekaProperties nonSecurePort(int nonSecurePort) {
        this.nonSecurePort = nonSecurePort;
        return this;
    }

    public FizzEurekaProperties instanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public String getInstanceId() {
        if (instanceId == null) {
            instanceId = ipAddress + ':' + appName + ':' + nonSecurePort;
        }
        return instanceId;
    }

    public FizzEurekaProperties preferIpAddress(boolean preferIpAddress) {
        this.preferIpAddress = preferIpAddress;
        return this;
    }

    public FizzEurekaProperties securePortEnabled(boolean securePortEnabled) {
        this.securePortEnabled = securePortEnabled;
        return this;
    }

    public FizzEurekaProperties healthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
        return this;
    }

    public String getHealthCheckUrl() {
        /*if (healthCheckUrl == null) {
            healthCheckUrl = "http://" + ipAddress + ':' + nonSecurePort + "/actuator/info";
        }*/
        return healthCheckUrl;
    }

    public FizzEurekaProperties region(String region) {
        this.region = region;
        return this;
    }

    public FizzEurekaProperties zone(String zone) {
        this.zone = zone;
        return this;
    }

    public FizzEurekaProperties serviceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        return this;
    }

    public FizzEurekaProperties securePort(int securePort) {
        this.securePort = securePort;
        return this;
    }
}
