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
package we.controller.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Get api config response entity
 *
 * @author zhongjie
 * @since 2.6.0
 */
@Data
public class GetApiConfigResp implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Api config infos
     */
    private List<ApiConfigInfo> apiConfigInfos;

    /**
     * Total count
     */
    private Long total;
}