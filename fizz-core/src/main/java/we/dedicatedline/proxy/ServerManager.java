/*
 *  Copyright (C) 2021 the original author or authors.
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
package we.dedicatedline.proxy;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import we.dedicatedline.proxy.server.ProxyServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * @author Francis Dong
 *
 */
@Component
public class ServerManager {

	private static final Logger log = LoggerFactory.getLogger(ServerManager.class);

//	private List<ProxyConfig> proxyConfigs;

	@Value("${dl.config}")
	private String dlconfig;

	@Resource
	private Environment environment;

	private Map<String, ProxyServer> serverMap = new HashMap<>();

	@PostConstruct
	public void initServers() {
		List<ProxyConfig> proxyConfigs = JSON.parseArray(dlconfig, ProxyConfig.class);
		for (ProxyConfig proxyConfig : proxyConfigs) {
			this.start(proxyConfig);
		}
//		ProxyConfig proxyConfig = new ProxyConfig(10001, "127.0.0.1", 8080, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@PreDestroy
	public void stopAllServers() throws InterruptedException {
		Set<Entry<String, ProxyServer>> serverSet = serverMap.entrySet();
		for (Iterator iterator = serverSet.iterator(); iterator.hasNext();) {
			Entry<String, ProxyServer> entry = (Entry<String, ProxyServer>) iterator.next();
			this.stop(entry.getValue().getProxyConfig());
		}
	}

	public void start(ProxyConfig proxyConfig) {
		// check if server exists
		if (serverMap.containsKey(proxyConfig.getServerPort().toString())) {
			log.warn("failed to start server, port({}) is already in used.", proxyConfig.getServerPort());
			return;
		}

		// start server
		ProxyServer proxyServer = null;
		try {
			proxyServer = new ProxyServer(proxyConfig);
			proxyServer.start();
		} catch (InterruptedException e) {
			log.error("failed to start server, port: {}", proxyConfig.getServerPort(), e);
			return;
		}
		serverMap.put(proxyConfig.getServerPort().toString(), proxyServer);
		log.info("server started, port: {}", proxyConfig.getServerPort());
	}

	public void stop(ProxyConfig proxyConfig) {
		// check if server exists
		if (!serverMap.containsKey(proxyConfig.getServerPort().toString())) {
			log.warn("server is not running, port: {}.", proxyConfig.getServerPort());
			return;
		}

		ProxyServer proxyServer = serverMap.get(proxyConfig.getServerPort().toString());
		try {
			proxyServer.stop();
			serverMap.remove(proxyConfig.getServerPort().toString());
		} catch (InterruptedException e) {
			log.error("failed to stop server, port: {}", proxyConfig.getServerPort(), e);
			return;
		}
		log.info("server stopped, port: {}", proxyConfig.getServerPort());
	}

}
