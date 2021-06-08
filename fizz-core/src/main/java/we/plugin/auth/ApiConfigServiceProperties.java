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
package we.plugin.auth;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * {@link ApiConfigService} properties
 *
 * @author zhongjie
 */
@RefreshScope
@Component
@Data
public class ApiConfigServiceProperties {
    @Value("${fizz-api-config.key:fizz_api_config_route}")
    private String fizzApiConfig;

    @Value("${fizz-api-config.channel:fizz_api_config_channel_route}")
    private String fizzApiConfigChannel;

    @Value("${need-auth:true}")
    private boolean needAuth;
}
