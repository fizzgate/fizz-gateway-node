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

/**
 * @author hongqiaowei
 */

public class FizzRefreshEvent extends ApplicationEvent {

    private Object event;

    private String eventDesc;

    public FizzRefreshEvent(Object source, Object event, String eventDesc) {
        super(source);
        this.event = event;
        this.eventDesc = eventDesc;
    }

    public Object getEvent() {
        return this.event;
    }

    public String getEventDesc() {
        return this.eventDesc;
    }
}
