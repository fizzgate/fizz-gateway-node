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
package com.fizzgate.controller.req;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.DigestUtils;

/**
 * Get api config detail request entity
 *
 * @author zhongjie
 * @since 2.6.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetApiConfigDetailReq extends BaseManagerConfigReq {
    /**
     * Api config ID
     */
    private Long apiConfigId;

    @Override
    boolean innerCheckSign(String key, String sign, Long timestamp) {
        return DigestUtils.md5DigestAsHex(String.format("%s-%s-%s", apiConfigId, timestamp, key).getBytes()).equals(sign);
    }
}
