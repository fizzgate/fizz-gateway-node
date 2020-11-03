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
import shell.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        Map configs = null;
        HashMap newConfig = null;
        try {
            configs = om.readValue(fileStream, Map.class);
            newConfig = new HashMap(configs);
            for (Object entry : configs.entrySet()) {
                Map.Entry<String,Object> entry1  = (Map.Entry<String,Object> )entry;
                if (entry1.getValue().toString().contains(" #")){
                    // Get Input
                    String[] parts =  entry1.getValue().toString().split(" #");
                    if (parts.length > 1){
                        String input = this.ask(parts[1]);
                        if (!input.isEmpty()){
                            newConfig.put(entry1.getKey(), input);
                        } else {
                            newConfig.put(entry1.getKey(), parts[0]);
                        }
                    }
                }
            }

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
}
