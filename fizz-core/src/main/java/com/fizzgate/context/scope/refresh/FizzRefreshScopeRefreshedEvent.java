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

package com.fizzgate.context.scope.refresh;

import org.springframework.context.ApplicationEvent;

/**
 * @author hongqiaowei
 */

public class FizzRefreshScopeRefreshedEvent extends ApplicationEvent {

    public static final String DEFAULT_NAME = "__refreshAll__"; // TODO

    private String name;

    public FizzRefreshScopeRefreshedEvent() {
        this(DEFAULT_NAME);
    }

    public FizzRefreshScopeRefreshedEvent(String name) {
        super(name);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
