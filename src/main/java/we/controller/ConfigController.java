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

package we.controller;

import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.fizz.ConfigLoader;
import we.util.ScriptUtils;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author unknown
 */

@RestController
@RequestMapping(value = "/config")
public class ConfigController {

    @Resource
    private ConfigLoader configLoader;

    @GetMapping("/reload")
    public Mono<String> reloadConfig(ServerWebExchange exchange) throws Exception {
        configLoader.init();
        return Mono.just("ok");
    }

    // add by hongqiaowei
    @PostMapping(value = "/fullUpdCommonJs", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> fullUpdCommonJs(ServerWebExchange exchange, @RequestBody String js) {
        try {
            File file = new File("js/common.js");
            file.delete();
            file = new File("js/common.js");
            FileUtils.writeStringToFile(file, js, StandardCharsets.UTF_8);
            ScriptUtils.recreateJavascriptEngineSignalMap.clear();
        } catch (Throwable t) {
            Mono.just(t.getMessage());
        }
        return Mono.just("done");
    }
}
