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

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * {@link RefreshLocalCacheConfig} properties
 *
 * @author zhongjie
 */
@RefreshScope
@Component
@Data
public class RefreshLocalCacheConfigProperties {

    @Value("${refresh-local-cache.api-config-enabled:false}")
    private boolean apiConfigCacheRefreshEnabled;

    @Value("${refresh-local-cache.api-config-2-apps-enabled:false}")
    private boolean apiConfig2AppsCacheRefreshEnabled;

    @Value("${refresh-local-cache.aggregate-config-enabled:false}")
    private boolean aggregateConfigCacheRefreshEnabled;

    @Value("${refresh-local-cache.gateway-group-enabled:false}")
    private boolean gatewayGroupCacheRefreshEnabled;

    @Value("${refresh-local-cache.app-auth-enabled:false}")
    private boolean appAuthCacheRefreshEnabled;

    @Value("${refresh-local-cache.flow-control-rule-enabled:false}")
    private boolean flowControlRuleCacheRefreshEnabled;

    @Value("${refresh-local-cache.rpc-service-enabled:false}")
    private boolean rpcServiceCacheRefreshEnabled;
}
