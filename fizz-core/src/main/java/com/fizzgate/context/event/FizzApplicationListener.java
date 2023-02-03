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

package com.fizzgate.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

/**
 * @author hongqiaowei
 */

public class FizzApplicationListener implements SmartApplicationListener {

    // private static final Logger LOGGER = LoggerFactory.getLogger(FizzApplicationListener.class);

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        // return ApplicationPreparedEvent.class.isAssignableFrom(eventType);
        return false;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        /*if (event instanceof ApplicationPreparedEvent) {
            ApplicationPreparedEvent evt = (ApplicationPreparedEvent) event;
            ConfigurableEnvironment environment = evt.getApplicationContext().getEnvironment();
            if (environment instanceof StandardReactiveWebEnvironment) {
            }
        }*/
    }
}
