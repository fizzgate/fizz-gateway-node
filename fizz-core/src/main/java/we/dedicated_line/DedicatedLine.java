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

package we.dedicated_line;

import cn.hutool.core.codec.Base64;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.CollectionUtils;
import we.util.JacksonUtils;

import java.util.*;

/**
 * @author hongqiaowei
 */

public class DedicatedLine {

    public boolean      isDeleted         = false;

    public String       pairCodeId;

    public String       secretKey;

    public String       requestCryptoKey;

    public String       customConfig;

    public List<ApiDoc> apiDocs           = Collections.emptyList();

    @JsonIgnore
    public Map<String/*service*/,
                                  Map<Object/*method*/, Set<String/*pathPattern*/>>
           >
           apiDocMap = Collections.emptyMap();

    public Set<String> servicesWithoutApiDocs = Collections.emptySet();

    public void setDeleted(int v) {
        if (v == 1) {
            isDeleted = true;
        }
    }

    public void setSecretKey(String sk) {
        secretKey = sk;
        int len = secretKey.length() / 2;
        requestCryptoKey = secretKey.substring(0, len);
        requestCryptoKey = Base64.encode(requestCryptoKey.getBytes());
    }

    public void setDocs(List<ApiDoc> docs) {
        apiDocs = docs;
        if (CollectionUtils.isEmpty(apiDocs)) {
            apiDocMap = Collections.emptyMap();
        } else {
            apiDocMap = new HashMap<>();
            for (ApiDoc apiDoc : apiDocs) {
                Map<Object, Set<String>> methodPathsMap = apiDocMap.computeIfAbsent(apiDoc.service, k -> new HashMap<>());
                for (MethodAndPath methodAndPath : apiDoc.methodAndPaths) {
                    Set<String> paths = methodPathsMap.computeIfAbsent(methodAndPath.method, k -> new HashSet<>());
                    paths.add(methodAndPath.path);
                }
            }
        }
    }

    public void setServices(Set<String> services) {
        servicesWithoutApiDocs = services;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
