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

package we.api.pairing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import we.config.SystemConfig;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = SystemConfig.FIZZ_API_PAIRING_CLIENT_ENABLE, havingValue = "true")
@Component
public class ApiPairingServiceRegistration implements ApplicationListener<FizzApiPairingWebServerInitializedEvent> {

    @Override
    public void onApplicationEvent(FizzApiPairingWebServerInitializedEvent event) {
    }
}
