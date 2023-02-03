package com.fizzgate.plugin.core.filter.config;

import java.lang.annotation.*;

import com.fizzgate.plugin.core.filter.config.parser.JsonParser;

import static java.lang.annotation.ElementType.TYPE;

/**
 * @author huanghua
 */
@Target(TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface FizzConfig {
    /**
     * 配置内容解析器
     */
    Class<? extends ContentParser> contentParser() default JsonParser.class;
}
