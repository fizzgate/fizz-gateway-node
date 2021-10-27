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

import we.util.JacksonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author hongqiaowei
 */

public class ApiPairingDocSet {

    public static final int DELETED = 1;

    public int                 isDeleted = 0;

    public long                id;

    public String              name;

    public String              description;

    public List<ApiPairingDoc> docs      = Collections.emptyList();

    public Set<String>         appIds    = Collections.emptySet();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiPairingDocSet that = (ApiPairingDocSet) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
