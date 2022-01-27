package we.plugin.core.filter.config;

import java.util.Map;

/**
 * @author huanghua
 */
public interface ContentParser {

    default <T> T parseRouterCfg(Map<String, Object> config, Class<T> toValueType) {
        throw new RuntimeException();
    }

    default <T> T parsePluginCfg(String source, Class<T> toValueType) {
        throw new RuntimeException();
    }

}
