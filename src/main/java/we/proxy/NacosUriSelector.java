package we.proxy;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * The Nacos implementation of {@code DiscoveryClientUriSelector}
 *
 * @author zhongjie
 */
@ConditionalOnProperty(value = "nacos.discovery.enabled")
@Service
public class NacosUriSelector extends AbstractDiscoveryClientUriSelector {
    private static final Logger log = LoggerFactory.getLogger(NacosUriSelector.class);

    @NacosInjected
    private NamingService naming;

    @Override
    public String getNextUri(String service, String relativeUri) {
        Instance instance = this.selectOneHealthyInstance(service);
        return super.buildUri(instance.getIp(), instance.getPort(), relativeUri);
    }

    private Instance selectOneHealthyInstance(String service) {
        Instance instance = null;
        try {
            instance = naming.selectOneHealthyInstance(service);
        } catch (NacosException e) {
            log.warn("Nacos selectOneHealthyInstance({}) exception", service, e);
        }

        if (instance == null) {
            throw new RuntimeException("Nacos no " + service, null, false, false) {};
        }

        return instance;
    }
}
