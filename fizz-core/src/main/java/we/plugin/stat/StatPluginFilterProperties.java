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

package we.plugin.stat;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import we.context.config.annotation.FizzRefreshScope;

/**
 * {@link StatPluginFilter} properties
 *
 * @author zhongjie
 */

@FizzRefreshScope
@Component
@Data
public class StatPluginFilterProperties {

    @Value("${stat.open:false}")
    private boolean statOpen = false;

    @Value("${stat.channel:fizz_access_stat_new}")
    private String fizzAccessStatChannel;

    @Value("${stat.topic:}")
    private String fizzAccessStatTopic;
}
