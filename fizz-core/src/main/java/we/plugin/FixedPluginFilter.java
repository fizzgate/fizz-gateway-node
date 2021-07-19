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

package we.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import we.FizzAppContext;
import we.util.ThreadContext;

import java.util.*;

/**
 * @author hongqiaowei
 */

@Deprecated
public abstract class FixedPluginFilter extends PluginFilter {

    private static final Logger log = LoggerFactory.getLogger(FixedPluginFilter.class);

    private static Map<String, FixedPluginFilter> fixedPluginFilterMap = new HashMap<>();

    private static List<FixedPluginFilter>        fixedPluginFilterList;

    private static void filters2sb(StringBuilder b) {
        int sz = fixedPluginFilterList.size() - 1;
        for (int i = 0; i < fixedPluginFilterList.size(); i++) {
            FixedPluginFilter f = fixedPluginFilterList.get(i);
            b.append(f.getId()).append('=').append(f);
            if (i != sz) {
                b.append('\n');
            }
        }
    }

    public static void add(FixedPluginFilter pf) {
        List<FixedPluginFilter> lst = new ArrayList<>();
        lst.addAll(fixedPluginFilterList);
        lst.add(pf);
        Collections.sort(lst,
                (fa, fb) -> {
                    return fa.getOrder() - fb.getOrder();
                }
        );
        String fid = pf.getId();
        fixedPluginFilterMap.put(fid, pf);
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append("add ").append(fid).append('\n');
        b.append("fixed plugin filters: \n");
        filters2sb(b);
        log.info(b.toString());
    }

    public static List<FixedPluginFilter> getPluginFilters() {
        if (fixedPluginFilterList == null) {
            synchronized (fixedPluginFilterMap) {
                if (fixedPluginFilterList == null) {
                    Map<String, FixedPluginFilter> beansOfType = FizzAppContext.appContext.getBeansOfType(FixedPluginFilter.class);
                    if (beansOfType == null || beansOfType.isEmpty()) {
                        fixedPluginFilterList = Collections.EMPTY_LIST;
                    } else {
                        fixedPluginFilterList = new ArrayList<>(beansOfType.values());
                        Collections.sort(fixedPluginFilterList,
                                (fa, fb) -> {
                                    return fa.getOrder() - fb.getOrder();
                                }
                        );
                        fixedPluginFilterList.forEach(
                                f -> {
                                    fixedPluginFilterMap.put(f.getId(), f);
                                }
                        );
                        StringBuilder b = ThreadContext.getStringBuilder();
                        b.append("fixed plugin filters: \n");
                        filters2sb(b);
                        log.info(b.toString());
                    }
                }
            }
        }
        return fixedPluginFilterList;
    }

    public abstract String getId();

    public abstract int    getOrder();

}
