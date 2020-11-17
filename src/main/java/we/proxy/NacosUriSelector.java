package we.proxy;

import com.alibaba.boot.nacos.discovery.properties.NacosDiscoveryProperties;
import com.alibaba.boot.nacos.discovery.properties.Register;
import com.alibaba.nacos.api.annotation.NacosInjected;
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
@ConditionalOnProperty(value = "nacos.discovery.enabled")
@Service
public class NacosUriSelector extends AbstractDiscoveryClientUriSelector {
    private static final Logger log = LoggerFactory.getLogger(NacosUriSelector.class);

    public NacosUriSelector(NacosDiscoveryProperties discoveryProperties) {
        this.discoveryProperties = discoveryProperties;
    }

    @NacosInjected
    private NamingService naming;
    private NacosDiscoveryProperties discoveryProperties;
    private String groupName;
    private List<String> clusterNameList;
    private boolean useGroupName;
    private boolean userClusterName;

    @PostConstruct
    public void init() {
        Register register = discoveryProperties.getRegister();
        if (register != null) {
            this.groupName = register.getGroupName();
            if (StringUtils.hasText(groupName)) {
                this.useGroupName = true;
            }
            String clusterName = register.getClusterName();
            if (StringUtils.hasText(clusterName)) {
                this.userClusterName = true;
                this.clusterNameList = Collections.singletonList(clusterName);
            }

        }
    }

    @Override
    public String getNextUri(String service, String relativeUri) {
        Instance instance = this.selectOneHealthyInstance(service);
        return super.buildUri(instance.getIp(), instance.getPort(), relativeUri);
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
            throw new RuntimeException("Nacos no " + service, null, false, false) {};
        }

        return instance;
    }
}
