package we.plugin.core.filter.config.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import we.plugin.core.filter.config.ContentParser;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author huanghua
 */
@Slf4j
public class JsonParser implements ContentParser {
    private static final Set<Class<?>> IGNORE_CONVERT_CLASS = Sets.newHashSet(
            String.class
            , Long.class
            , Integer.class
            , Double.class
            , Short.class
            , CharSequence.class
            , Character.class
            , BigDecimal.class
            , Boolean.class
    );
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = Maps.newHashMap();
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public <T> T parseRouterCfg(Map<String, Object> config, Class<T> toValueType) {
        ObjectNode jsonNode = objectMapper.convertValue(config, ObjectNode.class);
        convertConfig(jsonNode, toValueType);
        return objectMapper.convertValue(jsonNode, toValueType);
    }

    @Override
    public <T> T parsePluginCfg(String source, Class<T> toValueType) {
        try {
            return objectMapper.readValue(source, toValueType);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void convertConfig(ObjectNode jsonNode, Class<?> toValueType) {
        List<Field> cacheFields = fields(toValueType);
        for (Field field : cacheFields) {
            String fn = field.getName();
            if (jsonNode.has(fn) && !IGNORE_CONVERT_CLASS.contains(field.getType())) {
                List<Field> fs = fields(field.getType());
                if (fs.size() > 0) {
                    JsonNode node = jsonNode.get(fn);
                    JsonNode readTree = null;
                    try {
                        readTree = objectMapper.readTree(node.asText());
                    } catch (JsonProcessingException e) {
                        // ignore
                    }
                    if (readTree != null) {
                        jsonNode.put(fn, readTree);
                    }
                }
            }
        }
    }

    private List<Field> fields(Class<?> toValueType) {
        List<Field> cacheFields = FIELD_CACHE.get(toValueType);
        if (cacheFields == null) {
            // returns all members including private members but not inherited members.
            Field[] fields = toValueType.getDeclaredFields();
            cacheFields = Lists.newArrayList(fields);
            FIELD_CACHE.put(toValueType, cacheFields);
        }
        return cacheFields;
    }

}
