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

import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationEvent;

/**
 * @author hongqiaowei
 */

public class DedicatedLineWebServerInitializedEvent extends ApplicationEvent {

    private final ReactiveWebServerApplicationContext applicationContext;

    public DedicatedLineWebServerInitializedEvent(WebServer webServer, ReactiveWebServerApplicationContext applicationContext) {
        super(webServer);
        this.applicationContext = applicationContext;
    }

    @Override
    public WebServer getSource() {
        return (WebServer) super.getSource();
    }

    public ReactiveWebServerApplicationContext getApplicationContext() {
        return this.applicationContext;
    }
}
