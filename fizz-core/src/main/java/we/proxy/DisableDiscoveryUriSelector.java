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

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * The disable implementation of {@code DiscoveryClientUriSelector}, used when Nacos and Eureka discovery are not enabled.
 *
 * @author zhongjie
 */

@ConditionalOnExpression("${spring.cloud.nacos.discovery.enabled} == false and ${eureka.client.enabled} == false")
@Service
public class DisableDiscoveryUriSelector implements DiscoveryClientUriSelector {
    @Override
    public String getNextUri(String service, String relativeUri) {
        throw new RuntimeException("No " + service + " because discovery disabled", null, false, false) {};
    }

    @Override
    public ServiceInstance getNextInstance(String service) {
        throw new RuntimeException("No " + service + " because discovery disabled", null, false, false) {};
    }
}
