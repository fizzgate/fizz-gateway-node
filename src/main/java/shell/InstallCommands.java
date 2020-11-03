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

package shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import shell.service.InstallService;
import we.FizzGatewayApplication;

import java.io.File;
import java.io.InputStream;

/**
 * @author linwaiwai
 */

@ShellComponent
public class InstallCommands {
    @Autowired
    private ConfigurableApplicationContext ctx;
    @Autowired
    private InstallService installService;
    @ShellMethod("install application")
    public String install(){
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("application.yml");
        installService.template(fileStream);
        if (installService.install()){
            FizzGatewayApplication.restart(ctx);
            return "install done. if you want to run it background, use command: './boot start'. auto start to reboot for checking ...";
        } else {
            return "install failed";
        }
    }

    @ShellMethod("restart application")
    public String restart(){
        return "restart ...";
    }
}
