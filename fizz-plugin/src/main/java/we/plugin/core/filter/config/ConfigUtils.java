package we.plugin.core.filter.config;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author huanghua
 */
public abstract class ConfigUtils {
    public static final String DEFAULT_CHAR_MATCHER_ANY_OF = ",\n";

    public static Set<String> string2set(String strVal, String charMatcherAnyOf) {
        Set<String> finalSet = Sets.newHashSet();
        if (StringUtils.isBlank(strVal)) {
            return finalSet;
        }
        charMatcherAnyOf = StringUtils.isBlank(charMatcherAnyOf) ? DEFAULT_CHAR_MATCHER_ANY_OF : charMatcherAnyOf;
        Set<String> set = Sets.newHashSet(
                Splitter.on(CharMatcher.anyOf(charMatcherAnyOf)).trimResults().split(strVal));
        set = set.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        for (String s : set) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            finalSet.add(StringUtils.trimToEmpty(s));
        }
        return finalSet;
    }

    public static Set<String> string2set(String strVal) {
        return string2set(strVal, DEFAULT_CHAR_MATCHER_ANY_OF);
    }

}
