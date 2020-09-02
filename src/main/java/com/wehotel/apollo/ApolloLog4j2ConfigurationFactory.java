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

package com.wehotel.apollo;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationBuilder;

import java.net.URI;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

/**
 * log4j2初始化时通过该类从Apollo加载相应的log4j2的配置信息
 * 当通过Apollo修改log4j2配置后，能立即生效。例如，动态修改日志级别。
 * @author honam
 * @date 2019-08-05
 */
// @Plugin(name = "ApolloLog4j2ConfigurationFactory", category = ConfigurationFactory.CATEGORY)
// @Order(50)
public class ApolloLog4j2ConfigurationFactory extends ConfigurationFactory {

    private static final String LOG4J2_NAMESPACE = "log4j2";

    @Override
    protected String[] getSupportedTypes() {
        return new String[]{"*"};
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation) {
        // 从Apollo读取log4j2配置
        Config config = ConfigService.getConfig(LOG4J2_NAMESPACE);
        Set<String> propertyNames = config.getPropertyNames();

        Properties properties = new Properties();
        for (String propertyName : propertyNames) {
            String propertyValue = config.getProperty(propertyName, null);
            properties.setProperty(propertyName, propertyValue);
        }

        // 添加log4j2配置的监听器
        config.addChangeListener(new Log4j2ConfigChangeListener(properties));

        // 构造log4j2的Configuration
        return new PropertiesConfigurationBuilder()
                .setRootProperties(copyProperties(properties))
                .setLoggerContext(loggerContext)
                .build();
    }

    /**
     * 复制Properties
     *
     * @param properties 原Properties对象
     * @return 新Properties对象
     */
    @SuppressWarnings("unchecked")
    private Properties copyProperties(Properties properties) {
        Properties newProperties = new Properties();

        Enumeration<String> enumeration = (Enumeration<String>) properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String propertyName = enumeration.nextElement();
            newProperties.put(propertyName, properties.getProperty(propertyName));
        }
        return newProperties;
    }

    private class Log4j2ConfigChangeListener implements ConfigChangeListener {

        private Properties configProperties;

        Log4j2ConfigChangeListener(Properties configProperties) {
            this.configProperties = configProperties;
        }

        @Override
        public void onChange(ConfigChangeEvent changeEvent) {
            String newValue;
            ConfigChange configChange;

            for (String changedKey : changeEvent.changedKeys()) {
                configChange = changeEvent.getChange(changedKey);
                newValue = configChange.getNewValue();
                if (newValue != null) {
                    configProperties.put(changedKey, newValue);
                } else {
                    configProperties.remove(changedKey);
                }
            }

            // 构造新配置并应用到LoggerContext
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration newConfiguration = new PropertiesConfigurationBuilder()
                    .setRootProperties(copyProperties(configProperties))
                    .setLoggerContext(ctx)
                    .build();
            ctx.setConfiguration(newConfiguration);
        }
    }

}
