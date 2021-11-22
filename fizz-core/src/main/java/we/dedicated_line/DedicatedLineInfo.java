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

import we.util.JacksonUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author hongqiaowei
 */

public class DedicatedLineInfo {

    public boolean      isDeleted   = false;

    public String       id; // uuid

    public String       url;

    public String       pairCodeId;

    public String       secretKey;

    public String       requestCryptoKey;

    public List<String> services    = Collections.emptyList();

    public void setDeleted(int v) {
        if (v == 1) {
            isDeleted = true;
        }
    }

    public void setSecretKey(String sk) {
        secretKey = sk;
        int len = secretKey.length() / 2;
        requestCryptoKey = secretKey.substring(0, len);
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
