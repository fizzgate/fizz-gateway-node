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
 * {@link FlowStatSchedConfig} properties
 *
 * @author zhongjie
 */
@RefreshScope
@Component
@Data
public class FlowStatSchedConfigProperties {

    @Value("${flowControl:false}")
    private boolean flowControl;

    @Value("${flow-stat-sched.dest:redis}")
    private String dest;

    @Value("${flow-stat-sched.queue:fizz_resource_access_stat}")
    private String queue;
}
