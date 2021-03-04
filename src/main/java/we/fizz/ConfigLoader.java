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

package we.fizz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import com.alibaba.nacos.api.config.annotation.NacosValue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import we.config.AppConfigProperties;
import we.fizz.input.*;

import org.apache.commons.io.FileUtils;
import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import we.fizz.input.extension.dubbo.DubboInput;
import we.fizz.input.extension.mysql.MySQLInput;
import we.fizz.input.extension.request.RequestInput;
import we.fizz.input.extension.request.RequestInputConfig;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static we.config.AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE;
import static we.util.Constants.Symbol.FORWARD_SLASH;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author francis
 * @author zhongjie
 *
 */
@Component
public class ConfigLoader {
	@Autowired
	public ConfigurableApplicationContext appContext;
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);

	/**
	 * 聚合配置存放Hash的Key
	 */
	private static final String AGGREGATE_HASH_KEY = "fizz_aggregate_config";

	private static Map<String, String> aggregateResources = null;
	private static Map<String, ConfigInfo> resourceKey2ConfigInfoMap = null;
	private static Map<String, String> aggregateId2ResourceKeyMap = null;

	@Resource
	private AppConfigProperties appConfigProperties;

	@Resource(name = AGGREGATE_REACTIVE_REDIS_TEMPLATE)
	private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

	@NacosValue(value = "${fizz.aggregate.read-local-config-flag:false}", autoRefreshed = true)
	@Value("${fizz.aggregate.read-local-config-flag:false}")
	private Boolean readLocalConfigFlag;

	public Input createInput(String configStr) throws IOException {
		ONode cfgNode = ONode.loadStr(configStr);

		Input input = new Input();
		input.setName(cfgNode.select("$.name").getString());

		ClientInputConfig clientInputConfig = new ClientInputConfig();
		clientInputConfig.setDataMapping(cfgNode.select("$.dataMapping").toObject(Map.class));
		clientInputConfig.setHeaders(cfgNode.select("$.headers").toObject(Map.class));
		clientInputConfig.setMethod(cfgNode.select("$.method").getString());
		clientInputConfig.setPath(cfgNode.select("$.path").getString());
		if (clientInputConfig.getPath().startsWith(TEST_PATH_PREFIX)) {
			// always enable debug for testing
			clientInputConfig.setDebug(true);
		} else {
			if (cfgNode.select("$.debug") != null) {
				clientInputConfig.setDebug(cfgNode.select("$.debug").getBoolean());
			}
		}
		clientInputConfig.setType(InputType.valueOf(cfgNode.select("$.type").getString()));
		clientInputConfig.setLangDef(cfgNode.select("$.langDef").toObject(Map.class));
		clientInputConfig.setBodyDef(cfgNode.select("$.bodyDef").toObject(Map.class));
		clientInputConfig.setHeadersDef(cfgNode.select("$.headersDef").toObject(Map.class));
		clientInputConfig.setParamsDef(cfgNode.select("$.paramsDef").toObject(Map.class));
		clientInputConfig.setScriptValidate(cfgNode.select("$.scriptValidate").toObject(Map.class));
		clientInputConfig.setValidateResponse(cfgNode.select("$.validateResponse").toObject(Map.class));
		input.setConfig(clientInputConfig);
		return input;
	}

	public Pipeline createPipeline(String configStr) throws IOException {
		ONode cfgNode = ONode.loadStr(configStr);

		InputFactory.registerInput(RequestInput.TYPE, RequestInput.class);
		InputFactory.registerInput(MySQLInput.TYPE, MySQLInput.class);
		InputFactory.registerInput(DubboInput.TYPE, DubboInput.class);
		Pipeline pipeline = new Pipeline();
		pipeline.setApplicationContext(appContext);

		List<Map<String, Object>> stepConfigs = cfgNode.select("$.stepConfigs").toObject(List.class);
		for (Map<String, Object> stepConfig : stepConfigs) {
			// set the specified env URL
			this.handleRequestURL(stepConfig);
			SoftReference<Pipeline> weakPipeline = new SoftReference<Pipeline>(pipeline);
			Step step = new Step.Builder().read(stepConfig, weakPipeline);
			step.setName((String) stepConfig.get("name"));
			if (stepConfig.get("stop") != null) {
				step.setStop((Boolean) stepConfig.get("stop"));
			} else {
				step.setStop(false);
			}
			step.setDataMapping((Map<String, Object>) stepConfig.get("dataMapping"));
			pipeline.addStep(step);
		}

		return pipeline;
	}

	public List<ConfigInfo> getConfigInfo() {
		if (aggregateResources == null) {
			try {
				this.init();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<>(resourceKey2ConfigInfoMap.values());
	}

	public String getConfigStr(String configId) {
		if (aggregateResources == null) {
			try {
				this.init();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String resourceKey = aggregateId2ResourceKeyMap.get(configId);
		if (resourceKey == null) {
			return null;
		}
		return aggregateResources.get(resourceKey);
	}

	private void handleRequestURL(Map<String, Object> stepConfig) {
		List<Object> requests = (List<Object>) stepConfig.get("requests");
		for (Object obj : requests) {
			Map<String, Object> request = (Map<String, Object>) obj;
			String envUrl = (String) request.get(appConfigProperties.getEnv() + "Url");
			if (!StringUtils.isEmpty(envUrl)) {
				request.put("url", request.get(appConfigProperties.getEnv() + "Url"));
			}
		}
	}

	@PostConstruct
	public synchronized void init() throws Exception {
		if (aggregateResources == null) {
			aggregateResources = new ConcurrentHashMap<>(1024);
			resourceKey2ConfigInfoMap = new ConcurrentHashMap<>(1024);
			aggregateId2ResourceKeyMap = new ConcurrentHashMap<>(1024);
		}

		if (readLocalConfigFlag) {
			File dir = new File("json");
			if (dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles();
				if (files != null && files.length > 0) {
					for (File file : files) {
						if (!file.exists()) {
							throw new IOException("File not found");
						}
						String configStr = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
						this.addConfig(configStr);
					}
				}
			}
		} else {
			// 从Redis缓存中获取配置
			reactiveStringRedisTemplate.opsForHash().scan(AGGREGATE_HASH_KEY).subscribe(entry -> {
				String configStr = (String) entry.getValue();
				this.addConfig(configStr);
			});
		}
	}

	public synchronized void addConfig(String configStr) {
		if (aggregateResources == null) {
			try {
				this.init();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ONode cfgNode = ONode.loadStr(configStr);
		String method = cfgNode.select("$.method").getString();
		String path = cfgNode.select("$.path").getString();
		String resourceKey = method.toUpperCase() + ":" + path;
		String configId = cfgNode.select("$.id").getString();
		String configName = cfgNode.select("$.name").getString();
		long version = cfgNode.select("$.version").getLong();

		LOGGER.debug("add aggregation config, key={} config={}", resourceKey, configStr);
		if (StringUtils.hasText(configId)) {
			String existResourceKey = aggregateId2ResourceKeyMap.get(configId);
			if (StringUtils.hasText(existResourceKey)) {
				// 删除旧有的配置
				aggregateResources.remove(existResourceKey);
				resourceKey2ConfigInfoMap.remove(existResourceKey);
			}
			aggregateId2ResourceKeyMap.put(configId, resourceKey);
		}
		aggregateResources.put(resourceKey, configStr);
		resourceKey2ConfigInfoMap.put(resourceKey, this.buildConfigInfo(configId, configName, method, path, version));
	}

	public synchronized void deleteConfig(String configIds) {
		if (CollectionUtils.isEmpty(aggregateId2ResourceKeyMap)) {
			return;
		}

		JSONArray idArray = JSON.parseArray(configIds);
		idArray.forEach(it -> {
			String configId = (String) it;
			String existResourceKey = aggregateId2ResourceKeyMap.get(configId);
			if (StringUtils.hasText(existResourceKey)) {
				LOGGER.debug("delete aggregation config: {}", existResourceKey);
				aggregateResources.remove(existResourceKey);
				resourceKey2ConfigInfoMap.remove(existResourceKey);
				aggregateId2ResourceKeyMap.remove(configId);
			}
		});
	}

	public AggregateResource matchAggregateResource(String method, String path) {
		if (aggregateResources == null) {
			try {
				init();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		String key = method.toUpperCase() + ":" + path;
		if (aggregateResources.containsKey(key) && aggregateResources.get(key) != null) {
			String configStr = aggregateResources.get(key);
			Input input = null;
			Pipeline pipeline = null;
			try {
				input = createInput(configStr);
				pipeline = createPipeline(configStr);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			if (pipeline != null && input != null) {
				ClientInputConfig cfg = (ClientInputConfig) input.getConfig();
				return new AggregateResource(pipeline, input);
			}
		}
		return null;
	}

	private ConfigInfo buildConfigInfo(String configId, String configName, String method, String path, long version) {
		String serviceName = this.extractServiceName(path);
		ConfigInfo configInfo = new ConfigInfo();
		configInfo.setConfigId(configId);
		configInfo.setConfigName(configName);
		configInfo.setServiceName(serviceName);
		configInfo.setMethod(method);
		configInfo.setPath(path);
		configInfo.setVersion(version == 0 ? null : version);
		return configInfo;
	}

	private static final String FORMAL_PATH_PREFIX = "/proxy/";
	private static final int FORMAL_PATH_SERVICE_NAME_START_INDEX = 7;
	private static final String TEST_PATH_PREFIX = "/proxytest/";
	private static final int TEST_PATH_SERVICE_NAME_START_INDEX = 11;

	private String extractServiceName(String path) {
		if (path != null) {
			if (path.startsWith(FORMAL_PATH_PREFIX)) {
				int endIndex = path.indexOf(FORWARD_SLASH, FORMAL_PATH_SERVICE_NAME_START_INDEX);
				if (endIndex > FORMAL_PATH_SERVICE_NAME_START_INDEX) {
					return path.substring(FORMAL_PATH_SERVICE_NAME_START_INDEX, endIndex);
				}
			} else if (path.startsWith(TEST_PATH_PREFIX)) {
				int endIndex = path.indexOf(FORWARD_SLASH, TEST_PATH_SERVICE_NAME_START_INDEX);
				if (endIndex > TEST_PATH_SERVICE_NAME_START_INDEX) {
					return path.substring(TEST_PATH_SERVICE_NAME_START_INDEX, endIndex);
				}
			}
		}
		return null;
	}

	public static class ConfigInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		/**
		 * 配置ID
		 */
		private String configId;

		/**
		 * 配置名
		 */
		private String configName;

		/**
		 * 服务名
		 */
		private String serviceName;
		/**
		 * 接口请求method类型
		 */
		private String method;
		/**
		 * 接口请求路径
		 */
		private String path;
		/**
		 * 版本号
		 */
		private Long version;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ConfigInfo that = (ConfigInfo) o;
			return Objects.equals(configId, that.configId) && Objects.equals(configName, that.configName)
					&& Objects.equals(serviceName, that.serviceName) && Objects.equals(method, that.method)
					&& Objects.equals(path, that.path) && Objects.equals(version, that.version);
		}

		@Override
		public int hashCode() {
			return Objects.hash(configId, configName, serviceName, method, path, version);
		}

		public String getConfigId() {
			return configId;
		}

		public void setConfigId(String configId) {
			this.configId = configId;
		}

		public String getConfigName() {
			return configName;
		}

		public void setConfigName(String configName) {
			this.configName = configName;
		}

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}
}
