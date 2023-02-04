package com.fizzgate.beans.factory.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fizzgate.config.FizzConfigConfiguration;
import com.fizzgate.config.RedisReactiveProperties;
import com.fizzgate.context.event.FizzRefreshEvent;
import com.fizzgate.util.Consts;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ReactiveRedisHelper;
import com.fizzgate.util.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public class FizzEnvironmentPostProcessor implements EnvironmentPostProcessor, SmartApplicationListener {

    private static final DeferredLog LOGGER = new DeferredLog();

    private static Logger LOG = null;


    private ConfigurableEnvironment     environment;

    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String fizzConfigEnable = environment.getProperty("fizz.config.enable", Consts.S.TRUE);
        if (fizzConfigEnable.equals(Consts.S.TRUE)) {
            String host = environment.getProperty("aggregate.redis.host");
            String clusterNodes = environment.getProperty("aggregate.redis.clusterNodes");
            if (StringUtils.isNotBlank(host) || StringUtils.isNotBlank(clusterNodes)) {
                this.environment = environment;
                initReactiveStringRedisTemplate();
                initFizzPropertySource();
            }
        }
    }

    private void initReactiveStringRedisTemplate() {
        RedisReactiveProperties redisReactiveProperties = new RedisReactiveProperties() {
        };

        String host = environment.getProperty("aggregate.redis.host");
        if (StringUtils.isBlank(host)) {
            redisReactiveProperties.setType(RedisReactiveProperties.CLUSTER);
            redisReactiveProperties.setClusterNodes(environment.getProperty("aggregate.redis.clusterNodes"));
        } else {
            redisReactiveProperties.setHost(host);
            redisReactiveProperties.setPort(Integer.parseInt(environment.getProperty("aggregate.redis.port")));
            redisReactiveProperties.setDatabase(Integer.parseInt(environment.getProperty("aggregate.redis.database")));
        }

        String password = environment.getProperty("aggregate.redis.password");
        if (StringUtils.isNotBlank(password)) {
            redisReactiveProperties.setPassword(password);
        }

        reactiveStringRedisTemplate = ReactiveRedisHelper.getStringRedisTemplate(redisReactiveProperties);
    }

    private void initFizzPropertySource() {
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> sources = new HashMap<>();
        MapPropertySource fizzPropertySource = new MapPropertySource(FizzConfigConfiguration.PROPERTY_SOURCE, sources);
        propertySources.addFirst(fizzPropertySource);

        Result<?> result = Result.succ();
        Flux<Map.Entry<Object, Object>> fizzConfigs = reactiveStringRedisTemplate.opsForHash().entries("fizz_config");
        fizzConfigs.collectList()
                   .defaultIfEmpty(Collections.emptyList())
                   .flatMap(
                           es -> {
                               if (es.isEmpty()) {
                                   LOGGER.info("no fizz configs");
                               } else {
                                   String value = null;
                                   try {
                                       for (Map.Entry<Object, Object> e : es) {
                                           String key = (String) e.getKey();
                                           value = (String) e.getValue();
                                           Map<String, Object> config = JacksonUtils.readValue(value, new TypeReference<Map<String, Object>>(){});
                                           sources.put(key, config.get(key));
                                       }
                                   } catch (Throwable t) {
                                       result.code = Result.FAIL;
                                       result.msg  = "init fizz configs error, json: " + value;
                                       result.t    = t;
                                   }
                               }
                               return Mono.empty();
                           }
                   )
                   .onErrorReturn(
                           throwable -> {
                               result.code = Result.FAIL;
                               result.msg  = "init fizz configs error";
                               result.t    = throwable;
                               return true;
                           },
                           result
                   )
                   .block();

        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        if (!sources.isEmpty()) {
            LOGGER.info("fizz configs: " + JacksonUtils.writeValueAsString(sources));
        }

        String channel = "fizz_config_channel";
        reactiveStringRedisTemplate.listenToChannel(channel)
                                   .doOnError(
                                           t -> {
                                               result.code = Result.FAIL;
                                               result.msg  = "lsn " + channel + " channel error";
                                               result.t    = t;
                                               LOGGER.error("lsn channel " + channel + " error", t);
                                           }
                                   )
                                   .doOnSubscribe(
                                           s -> {
                                               LOGGER.info("success to lsn on " + channel);
                                           }
                                   )
                                   .doOnNext(
                                           msg -> {
                                               if (LOG == null) {
                                                   LOG = LoggerFactory.getLogger(FizzEnvironmentPostProcessor.class);
                                               }
                                               String message = msg.getMessage();
                                               try {
                                                   Map<String, Object> changedPropertyValueMap = new HashMap<>();
                                                   List<Map<String, Object>> changes = JacksonUtils.readValue(message, new TypeReference<List<Map<String, Object>>>(){});
                                                   for (Map<String, Object> change : changes) {
                                                       int isDeleted = (int) change.remove("isDeleted");
                                                       Map.Entry<String, Object> propertyValue = change.entrySet().iterator().next();
                                                       String property = propertyValue.getKey();
                                                       Object v = null;
                                                       if (isDeleted == 1) {
                                                           sources.remove(property);
                                                       } else {
                                                           v = propertyValue.getValue();
                                                           sources.put(property, v);
                                                       }
                                                       changedPropertyValueMap.put(property, v);
                                                   }
                                                   LOG.info("new fizz configs: " + JacksonUtils.writeValueAsString(sources));
                                                   ApplicationContext applicationContext = FizzBeanFactoryPostProcessor.getApplicationContext();
                                                   FizzRefreshEvent refreshEvent = new FizzRefreshEvent(applicationContext, FizzRefreshEvent.ENV_CHANGE, changedPropertyValueMap);
                                                   applicationContext.publishEvent(refreshEvent);
                                               } catch (Throwable t) {
                                                   LOG.error("update fizz config " + message + " error", t);
                                               }
                                           }
                                   )
                                   .subscribe();

        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationPreparedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationPreparedEvent) {
            LOGGER.replayTo(FizzEnvironmentPostProcessor.class);
        }
    }
}