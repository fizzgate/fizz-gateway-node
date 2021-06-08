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
 * {@link WebFluxConfig} properties
 *
 * @author zhongjie
 */
@RefreshScope
@Component
@Data
public class WebFluxConfigProperties {
    /**
     * Configure the maximum amount of disk space allowed for file parts. Default 100M (104857600)
     */
    @Value(             "${server.fileUpload.maxDiskUsagePerPart:104857600}"                      )
    private long maxDiskUsagePerPart;

    /**
     * Maximum parts of multipart form-data, including form field parts; Default -1 no limit
     */
    @Value(             "${server.fileUpload.maxParts:-1}"                      )
    private int maxParts;
}
