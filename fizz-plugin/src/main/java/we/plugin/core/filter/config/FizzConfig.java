package we.plugin.core.filter.config;

import we.plugin.core.filter.config.parser.JsonParser;

import java.lang.annotation.*;

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
