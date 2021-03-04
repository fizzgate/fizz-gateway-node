package we.proxy.dubbo;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DubboUtils {
    public static boolean isEmpty(final String body) {
        return null == body || "".equals(body) || "{}".equals(body) || "null".equals(body);
    }
    /*
     * body json string
     */
    public static Pair<String[], Object[]> parseDubboParam(String body, final String parameterTypes) {

        Map<String, Object> paramMap = (Map<String,Object>) JSON.parse(body);
        String[] parameter = StringUtils.split(parameterTypes, ',');
        if (parameter.length == 1 && !isBaseType(parameter[0])) {
            return new ImmutablePair<>(parameter, new Object[]{paramMap});
        }
        List<Object> list = new LinkedList<>();
        for (String key : paramMap.keySet()) {
            Object obj = paramMap.get(key);
            if (obj != null) {
                list.add(obj);
            }
        }
        Object[] objects = list.toArray();
        return new ImmutablePair<>(parameter, objects);
    }

    private static boolean isBaseType(String type) {
        return type.startsWith("java") || type.startsWith("[Ljava");
    }
}
