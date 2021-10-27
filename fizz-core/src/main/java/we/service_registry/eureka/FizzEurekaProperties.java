package we.service_registry.eureka;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;

public class FizzEurekaProperties {

    public String id;

    public String appName;

    public String virtualHostName;

    public String ipAddress;

    public int nonSecurePort = 80;

    public String instanceId;

    public boolean preferIpAddress = true;

    public boolean securePortEnabled = false;

    public String healthCheckUrl;

    public DataCenterInfo dataCenterInfo = new MyDataCenterInfo(
            DataCenterInfo.Name.MyOwn);

    public String region = "default";

    public String zone = EurekaClientConfigBean.DEFAULT_ZONE;

    public String serviceUrl;

    public int securePort = 443;

    public FizzEurekaProperties id(String id) {
        this.id = id;
        return this;
    }

    public FizzEurekaProperties appName(String appName) {
        this.appName = appName;
        return this;
    }

    public FizzEurekaProperties virtualHostName(String virtualHostName) {
        this.virtualHostName = virtualHostName;
        return this;
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
