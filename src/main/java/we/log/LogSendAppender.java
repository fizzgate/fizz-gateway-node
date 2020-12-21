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
package we.log;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import we.FizzAppContext;
import we.flume.clients.log4j2appender.LogService;
import we.util.NetworkUtils;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * log send appender
 *
 * @author zhongjie
 */
@Plugin(name = LogSendAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class LogSendAppender extends AbstractAppender {

    static final String PLUGIN_NAME = "LogSend";
    public static LogSendService logSendService;
    public static Boolean logEnabled;
    private static LogSend[] logSends = new LogSend[1000];
    private static AtomicInteger logSendIndex = new AtomicInteger(0);

    private LogSendAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @Override
    public void append(LogEvent event) {
        if (logEnabled != null && !logEnabled) {
            return;
        }

        if (logEnabled == null && FizzAppContext.appContext == null && logSendService == null) {
            // local cache
            logSends[logSendIndex.getAndIncrement() % logSends.length] = new LogSend(this.getBizId(event.getMessage().getParameters()),
                    NetworkUtils.getServerIp(), event.getLevel().intLevel(), event.getTimeMillis(), new String(this.getLayout().toByteArray(event)));
            return;
        }

        if (logEnabled == null && logSendService == null) {
            // no legal logSendService, discard the local cache
            logEnabled = Boolean.FALSE;
            logSends = null;
            return;
        }

        if (logEnabled == null) {
            logEnabled = Boolean.TRUE;

            LogSend[] logSends;
            synchronized (LogSendAppender.class) {
                logSends = LogSendAppender.logSends;
                LogSendAppender.logSends = null;
            }

            // logSendService is ready, send the local cache
            if (logSends != null) {
                int size = Math.min(logSendIndex.get(), logSends.length);
                for (int i = 0; i < size; i++) {
                    logSendService.send(logSends[i]);
                }
            }
        }

        LogSend logSend = new LogSend(this.getBizId(event.getMessage().getParameters()), NetworkUtils.getServerIp(),
                event.getLevel().intLevel(), event.getTimeMillis(), new String(this.getLayout().toByteArray(event)));
        logSendService.send(logSend);
    }

    private String getBizId(Object[] parameters) {
        Object bizId = LogService.getBizId();
        if (parameters != null) {
            for (int i = parameters.length - 1; i > -1; --i) {
                Object p = parameters[i];
                if (p == LogService.BIZ_ID) {
                    if (i != parameters.length - 1) {
                        bizId = parameters[i + 1];
                    }
                    break;
                }
            }
        }
        if (bizId == null) {
            return "";
        }
        return bizId.toString();
    }

    @PluginFactory
    public static LogSendAppender createAppender(@PluginAttribute("name") String name,
                                                 @PluginElement("Filter") final Filter filter,
                                                 @PluginElement("Layout") Layout<? extends Serializable> layout,
                                                 @PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
        if (name == null) {
            LOGGER.error("No name provided for LogSendAppender!");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new LogSendAppender(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }
}
