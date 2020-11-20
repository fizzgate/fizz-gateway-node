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
package shell.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import shell.yaml.YAMLFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;

/**
 * @author linwaiwai
 */

@Service
public class InstallService {
    @Autowired
    LineReader reader;

    public String ask(String question) {
        return this.reader.readLine("\n" + question + " > ");
    }
    private InputStream fileStream;
    public Boolean install(){
        ObjectMapper om = new ObjectMapper(new YAMLFactory().enable(MINIMIZE_QUOTES));
        Map configs = null;
        Map newConfig = null;
        try {
            byte[] bytes = StreamUtils.copyToByteArray(fileStream);
            configs = om.readValue(new ByteArrayInputStream(bytes), Map.class);
            newConfig = om.readValue(new ByteArrayInputStream(bytes), Map.class);

            this.handleConfigurableItem(configs, newConfig);

            File distFile = new File(getConfigPath());
            om.writeValue(distFile, newConfig);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    static public String getConfigPath(){
        String configPath = System.getProperty("user.home");
        return configPath + "/application.yml";
    }

    static public boolean shouldInstall(){
        File distFile = new File(getConfigPath());
        return !distFile.exists() ;
    }

    public void template(InputStream fileStream) {
        this.fileStream = fileStream;
    }

    private void handleConfigurableItem(Map configs, Map newConfig) {
        this.doHandleConfigurableItem(configs, newConfig, "", new HashSet<String>(8), new HashSet<String>(8));
    }

    private void doHandleConfigurableItem(Map configs, Map newConfig, String itemPrefix, Set<String> enabledPrefixSet, Set<String> disabledPrefixSet) {
        for (Object entry : configs.entrySet()) {
            Map.Entry<String,Object> entry1  = (Map.Entry<String,Object> )entry;
            String key = entry1.getKey();
            Object value = entry1.getValue();
            if (value instanceof Map) {
                this.doHandleConfigurableItem((Map) value, (Map) newConfig.get(key),
                        StringUtils.hasText(itemPrefix) ? itemPrefix + "." + key : key, enabledPrefixSet, disabledPrefixSet);
            } else {
                if (value.toString().contains(" #")) {
                    // Get Input
                    String[] parts = value.toString().split(" #");
                    if (parts.length > 1) {
                        if (enabledPrefixSet.stream().anyMatch(itemPrefix::startsWith)) {
                            if (parts[1].startsWith("use ")) {
                                newConfig.put(key, true);
                                continue;
                            }
                        }
                        if (disabledPrefixSet.stream().anyMatch(itemPrefix::startsWith)) {
                            if (parts[1].startsWith("use ")) {
                                newConfig.put(key, false);
                            } else {
                                newConfig.put(key, parts[0]);
                            }
                            continue;
                        }

                        String input = this.ask(parts[1]).trim();
                        Object newValue;
                        if (!input.isEmpty()) {
                            newValue = input;
                        } else {
                            newValue = parts[0];
                        }

                        if (newValue.equals("true")) {
                            newValue = Boolean.TRUE;
                        } else if (newValue.equals("false")) {
                            newValue = Boolean.FALSE;
                        } else if (newValue.equals("null")) {
                            newValue = null;
                        }

                        newConfig.put(key, newValue);

                        if (parts[1].startsWith("use ")) {
                            assert newValue instanceof Boolean;
                            if ((Boolean)newValue) {
                                enabledPrefixSet.add(itemPrefix);
                            } else {
                                disabledPrefixSet.add(itemPrefix);
                            }
                        }
                    }
                }
            }
        }
    }
}
