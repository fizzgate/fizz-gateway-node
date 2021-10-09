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

package we.dict;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import we.util.JacksonUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public class Dict {

    public static final int BOOLEAN = 1;
    public static final int STRING  = 2;
    public static final int NUMBER  = 3;
    public static final int JSON    = 4;

    public static final int DELETED = 1;

    public int        isDeleted = 0;

    public int        id;

    public String     key;

    public int        type;

    public String     value;

    public boolean    booleanVal;

    public String     stringVal;

    public BigDecimal numberVal;

    public int        intVal;

    public long       longVal;

    public float      floatVal;

    public double     doubleVal;

    public String     jsonVal;

    public Map<String, Object> valMap;

    public List<Object> valList;

    public long       create;

    public long       update;

    @JsonCreator
    public Dict(
            @JsonProperty("isDeleted") int     isDeleted,
            @JsonProperty("id")        int     id,
            @JsonProperty("key")       String  key,
            @JsonProperty("type")      int     type,
            @JsonProperty("value")     String  value,
            @JsonProperty("create")    long    create,
            @JsonProperty("update")    long    update
            ) {
        this.isDeleted = isDeleted;
        this.id        = id;
        this.key       = key;
        this.type      = type;
        this.value     = value;
        this.create    = create;
        this.update    = update;
        if (type == BOOLEAN) {
            booleanVal = Boolean.parseBoolean(value);
        } else if (type == STRING) {
            stringVal = value;
        } else if (type == NUMBER) {
            numberVal = new BigDecimal(value);
            if (value.indexOf('.') == -1) {
                intVal  = numberVal.intValue();
                longVal = numberVal.longValue();
            } else {
                floatVal  = numberVal.floatValue();
                doubleVal = numberVal.doubleValue();
            }
        } else { // JSON
            jsonVal = value;
            if (value.startsWith("{")) {
                valMap  = JacksonUtils.readValue(jsonVal, Map.class);
            } else {
                valList = JacksonUtils.readValue(jsonVal, List.class);
            }
        }
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
