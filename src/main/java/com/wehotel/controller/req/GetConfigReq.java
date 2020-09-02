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

package com.wehotel.controller.req;

import org.springframework.util.DigestUtils;

import java.util.Objects;

/**
 * 获取聚合配置请求实体类
 * @author zhongjie
 */
public class GetConfigReq extends BaseManagerConfigReq {
    /**
     * 服务名
     */
    private String serviceName;
    /**
     * 接口名
     */
    private String name;
    /**
     * 接口请求路径
     */
    private String path;

    @Override
    boolean innerCheckSign(String key, String sign, Long timestamp) {
        return DigestUtils.md5DigestAsHex(
                String.format("%s-%s-%s-%s-%s", serviceName, name, path, timestamp, key).getBytes()).equals(sign);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GetConfigReq that = (GetConfigReq) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(name, that.name) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), serviceName, name, path);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
