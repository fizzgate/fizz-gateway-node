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

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * extend AbstractCustomAuth instead implement this class.
 *
 * @author hongqiaowei
 */

public interface CustomAuth {

    /**
     * 认证通过返回 Mono<Access.YES>, 不通过返回 Mono<Access.CUSTOM_AUTH_REJECT>
     *
     * @deprecated
     */
    @Deprecated
    Mono<ApiConfigService.Access> auth(ServerWebExchange exchange, String appId, String ip, String timestamp, String sign, App fizzAppConfig);
}
