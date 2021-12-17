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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Applications;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.util.CollectionUtils;
import we.service_registry.FizzServiceRegistration;
import we.util.Consts;
import we.util.Utils;

import java.util.List;

/**
 * @author hongqiaowei
 */

public class FizzEurekaServiceRegistration extends FizzServiceRegistration {

    private final CloudEurekaClient client;

    public FizzEurekaServiceRegistration(String id, EurekaRegistration registration, EurekaServiceRegistry serviceRegistry, CloudEurekaClient client) {
        super(id, registration, serviceRegistry);
        this.client = client;
    }

    @Override
    public String getInstance(String service) {
        InstanceInfo inst = getInstanceInfo(service);
        return inst.getIPAddr() + Consts.S.COLON + inst.getPort();
    }

    public InstanceInfo getInstanceInfo(String service) {
        List<InstanceInfo> insts = client.getInstancesByVipAddress(service, false);
        if (CollectionUtils.isEmpty(insts)) {
            throw Utils.runtimeExceptionWithoutStack(id + " eureka no " + service);
        }
        Applications apps = client.getApplications();
        int index = (int) (apps.getNextIndex(service.toUpperCase(), false).incrementAndGet() % insts.size());
        return insts.get(index);
    }
}
