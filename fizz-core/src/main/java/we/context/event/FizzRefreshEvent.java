/*
 *  Copyright (C) 2021 the original author or authors.
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

package we.context.event;

import org.springframework.context.ApplicationEvent;
import we.util.JacksonUtils;

/**
 * @author hongqiaowei
 */

public class FizzRefreshEvent extends ApplicationEvent {

    public static final byte ENV_CHANGE = 1;

    private final byte   type;

    private final Object data;

    public FizzRefreshEvent(Object source, byte type, Object data) {
        super(source);
        this.type = type;
        this.data = data;
    }

    public byte getType() {
        return this.type;
    }

    public Object getData() {
        return this.data;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
