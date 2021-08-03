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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Applications;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Eureka implementation of {@code DiscoveryClientUriSelector}
 *
 * @author zhongjie
 */
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@Service
public class EurekaUriSelector extends AbstractDiscoveryClientUriSelector {

    @Resource
    private EurekaClient eurekaClient;

    @Override
    public String getNextUri(String service, String relativeUri) {
        InstanceInfo inst = roundRobinChoose1instFrom(service);
        return buildUri(inst.getIPAddr(), inst.getPort(), relativeUri);
    }

    @Override
    public ServiceInstance getNextInstance(String service) {
        InstanceInfo inst = roundRobinChoose1instFrom(service);
        return new ServiceInstance(inst.getIPAddr(), inst.getPort());
    }


    // private static List<InstanceInfo> aggrMemberInsts = new ArrayList<>();
    // static {
    //     InstanceInfo i0 = InstanceInfo.Builder.newBuilder().setAppName("MINITRIP").setIPAddr("xxx.xxx.63.192").setPort(7094).build();
    //     aggrMemberInsts.add(i0);
    // }
    // private static AtomicLong counter = new AtomicLong(0);
    // private static final String aggrMember = "minitrip";


    private InstanceInfo roundRobinChoose1instFrom(String service) {

        // if (aggrMember.equals(service)) {
        //     int idx = (int) (counter.incrementAndGet() % aggrMemberInsts.size());
        //     return aggrMemberInsts.get(idx);
        // }

        List<InstanceInfo> insts = eurekaClient.getInstancesByVipAddress(service, false);
        if (insts == null || insts.isEmpty()) {
            throw new RuntimeException("eureka no " + service, null, false, false) {};
        }
        Applications apps = eurekaClient.getApplications();
        int index = (int) (apps.getNextIndex(service.toUpperCase(), false).incrementAndGet() % insts.size());
        return insts.get(index);
    }
}
