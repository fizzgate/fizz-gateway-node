package we.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import we.log.LogProperties;
import we.log.LogSendAppenderWithLogback;

@Configuration
public class LogConfig {

    @Bean
    @ConfigurationProperties("fizz.logging")
    public LogProperties logProperties() {
        return new LogProperties();
    }

    @Configuration
    @ConditionalOnClass(AbstractAppender.class)
    @AutoConfigureAfter(AggregateRedisConfig.class)
    public static class CustomLog4j2Config {
    }

    @Configuration
    @ConditionalOnClass(LoggerContext.class)
    @AutoConfigureAfter(AggregateRedisConfig.class)
    public static class CustomLogbackConfig {
        @Bean
        public Object initLogSendAppenderWithLogback(LogProperties logProperties) {
            return new LoggingConfigurationApplicationListener(logProperties);
        }
    }

    public static class LoggingConfigurationApplicationListener {
        private static final Logger logger = LoggerFactory.getLogger(LoggingConfigurationApplicationListener.class);
        private static final String APPENDER_NAME = "fizzLogSendAppender";
        private static final String LAYOUT = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %level %logger{36} - %msg%n";
        private LogProperties logProperties;

        public LoggingConfigurationApplicationListener() {
        }

        public LoggingConfigurationApplicationListener(LogProperties logProperties) {
            this.logProperties = logProperties;
        }

        @EventListener
        public void contextRefreshed(SpringApplicationEvent event) {
            onApplicationEvent(event);
        }

        @EventListener
        public void applicationStarting(ApplicationStartingEvent event) {
            onApplicationEvent(event);
        }

        @EventListener
        public void applicationReady(ApplicationReadyEvent event) {
            onApplicationEvent(event);
        }

        public void onApplicationEvent(ApplicationEvent event) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            final ch.qos.logback.classic.Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            String layoutConfig = StringUtils.isBlank(logProperties.getLayout()) ? LAYOUT : logProperties.getLayout();

            final LogSendAppenderWithLogback newAppender = new LogSendAppenderWithLogback();
            newAppender.setName(APPENDER_NAME);
            newAppender.setContext(context);
            PatternLayout layout = new PatternLayout();
            layout.setPattern(layoutConfig);
            newAppender.setLayout(layout);

            Appender<ILoggingEvent> appender = root.getAppender(APPENDER_NAME);
            if (appender == null) {
                newAppender.start();
                root.addAppender(newAppender);
                logger.info("Added fizz log send appender:{}", APPENDER_NAME);
            } else {
                newAppender.start();
                root.detachAppender(APPENDER_NAME);
                root.addAppender(newAppender);
                logger.info("Refresh fizz log send appender:{}", APPENDER_NAME);
            }
        }
    }


}
