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

package we.proxy;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import we.util.JacksonUtils;
import we.util.Utils;

import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public class CallbackReplayReq {

    public static interface Type {
        static final int ORIGINAL_PATH   = 1;
        static final int ASSIGN_SERVICES = 2;
    }

    public String id;

    public String app;

    public String gatewayGroup;

    public HttpMethod method;

    public String service;

    public String path;

    public String query;

    public HttpHeaders headers;

    public String body;

    public int replayType;

    public Map<String, ServiceInstance> receivers;

    public List<ServiceTypePath> assignServices;

    public void setMethod(String m) {
        method = HttpMethod.resolve(m);
    }

    public void setHeaders(Map<String, List<String>> hs) {
        if (hs != null && !hs.isEmpty()) {
            headers = new HttpHeaders();
            hs.forEach(
                    (h, vs) -> {
                        headers.addAll(h, vs);
                    }
            );
        }
    }

    public void setReceivers(String rs) {
        if (StringUtils.isNotBlank(rs)) {
            try {
                receivers = JacksonUtils.getObjectMapper().readValue(rs, new TypeReference<Map<String, ServiceInstance>>(){});
            } catch (JsonProcessingException e) {
                throw Utils.runtimeExceptionWithoutStack(rs + " receivers str invalid");
            }
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
