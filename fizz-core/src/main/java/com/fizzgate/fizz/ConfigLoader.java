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

package com.fizzgate.fizz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.fizzgate.config.AppConfigProperties;
import com.fizzgate.fizz.input.ClientInputConfig;
import com.fizzgate.fizz.input.Input;
import com.fizzgate.fizz.input.InputFactory;
import com.fizzgate.fizz.input.InputType;
import com.fizzgate.util.Consts;
import com.fizzgate.util.ReactorUtils;

import com.fizzgate.util.UrlTransformUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.ThreadContext;
import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.fizzgate.config.AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE;
import static com.fizzgate.util.Consts.S.FORWARD_SLASH;
import static com.fizzgate.util.Consts.S.FORWARD_SLASH_STR;

/**
 * 
 * @author Francis Dong
 * @author zhongjie
 *
 */
@Component
public class ConfigLoader {
	/**
	 * legacy aggregate formal path prefix
	 */
	private static final String LEGACY_FORMAL_PATH_PREFIX = "/proxy";
	/**
	 * legacy aggregate test path prefix
	 */
	private static final String LEGACY_TEST_PATH_PREFIX = "/proxytest";
	/**
	 * aggregate test path prefix
	 */
	private static final String TEST_PATH_PREFIX = "/_proxytest";
	/**
	 * aggregate test path service name start index
	 */
	private static final int TEST_PATH_SERVICE_NAME_START_INDEX = TEST_PATH_PREFIX.length() + 1;

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

	@Resource
	private ConfigLoaderProperties configLoaderProperties;

	private String formalPathPrefix;
	private int formalPathServiceNameStartIndex;

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
		clientInputConfig.setContentType(cfgNode.select("$.contentType").getString());
		clientInputConfig.setXmlArrPaths(cfgNode.select("$.xmlArrPaths").getString());
		input.setConfig(clientInputConfig);
		return input;
	}

	public Pipeline createPipeline(String configStr) throws IOException {
		ONode cfgNode = ONode.loadStr(configStr);

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
		this.refreshLocalCache();
		InputFactory.loadInputClasses();
	}


	public synchronized  void refreshLocalCache() throws Exception {
		if (formalPathPrefix == null) {
			String formalPathPrefixTmp = appContext.getEnvironment().getProperty("gateway.prefix", "/proxy");
			if (formalPathPrefixTmp.endsWith(FORWARD_SLASH_STR)) {
				// remove the end slash
				formalPathPrefixTmp = formalPathPrefixTmp.substring(0, formalPathPrefixTmp.length() - 1);
			}
			formalPathPrefix = formalPathPrefixTmp;
			formalPathServiceNameStartIndex = formalPathPrefix.length() + 1;
		}

        Map<String, String> aggregateResourcesTmp = new ConcurrentHashMap<>(1024);
        Map<String, ConfigInfo> resourceKey2ConfigInfoMapTmp = new ConcurrentHashMap<>(1024);
        Map<String, String> aggregateId2ResourceKeyMapTmp = new ConcurrentHashMap<>(1024);

		if (configLoaderProperties.getReadLocalConfigFlag()) {
			File dir = new File("json");
			if (dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles();
				if (files != null && files.length > 0) {
					for (File file : files) {
						if (!file.exists()) {
							throw new IOException("File not found");
						}
						String configStr = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
						this.addConfig(configStr, aggregateResourcesTmp, resourceKey2ConfigInfoMapTmp, aggregateId2ResourceKeyMapTmp);
					}
				}
			}
		} else {
			// 从Redis缓存中获取配置
			final Throwable[] throwable = new Throwable[1];
			Throwable error = Mono.just(Objects.requireNonNull(reactiveStringRedisTemplate.opsForHash().entries(AGGREGATE_HASH_KEY)
					.defaultIfEmpty(new AbstractMap.SimpleEntry<>(ReactorUtils.OBJ, ReactorUtils.OBJ)).onErrorStop().doOnError(t -> LOGGER.info(null, t))
					.concatMap(entry -> {
						Object k = entry.getKey();
						if (k == ReactorUtils.OBJ) {
							return Flux.just(entry);
						}
						String configStr = (String) entry.getValue();
						// LOGGER.info("aggregate config: " + k.toString() + Consts.S.COLON + configStr, LogService.BIZ_ID, k.toString());

						ThreadContext.put(Consts.TRACE_ID, k.toString());
						LOGGER.info("aggregate config: " + k.toString() + Consts.S.COLON + configStr);

						try {
							this.addConfig(configStr, aggregateResourcesTmp, resourceKey2ConfigInfoMapTmp, aggregateId2ResourceKeyMapTmp);
							return Flux.just(entry);
						} catch (Throwable t) {
							throwable[0] = t;
							LOGGER.info(configStr, t);
							return Flux.error(t);
						}
					}).blockLast())).flatMap(
					e -> {
						if (throwable[0] != null) {
							return Mono.error(throwable[0]);
						}
						return Mono.just(ReactorUtils.EMPTY_THROWABLE);
					}
			).block();
			if (error != ReactorUtils.EMPTY_THROWABLE) {
				assert error != null;
				throw new RuntimeException(error);
			}
		}

        aggregateResources = aggregateResourcesTmp;
        resourceKey2ConfigInfoMap = resourceKey2ConfigInfoMapTmp;
        aggregateId2ResourceKeyMap = aggregateId2ResourceKeyMapTmp;
    }

	public synchronized void addConfig(String configStr) {
        if (aggregateResources == null) {
            try {
                this.init();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

	    this.addConfig(configStr, aggregateResources, resourceKey2ConfigInfoMap, aggregateId2ResourceKeyMap);
    }

    private void addConfig(String configStr, Map<String, String> aggregateResources,
                           Map<String, ConfigInfo> resourceKey2ConfigInfoMap, Map<String, String> aggregateId2ResourceKeyMap) {
		ONode cfgNode = ONode.loadStr(configStr);

		boolean needReGenConfigStr = false;
		// in the future aggregate config will add this field and remove the prefix '/proxy'|'/proxytest' of path
		boolean existAggrVersion = cfgNode.contains("aggrVersion");

		String method = cfgNode.select("$.method").getString();
		String path = cfgNode.select("$.path").getString();

		if (!existAggrVersion) {
			if (path.startsWith(LEGACY_TEST_PATH_PREFIX)) {
				// legacy test path, remove prefix '/proxytest'
				path = path.replaceFirst(LEGACY_TEST_PATH_PREFIX, TEST_PATH_PREFIX);
				needReGenConfigStr = true;
			} else if (path.startsWith(LEGACY_FORMAL_PATH_PREFIX)) {
				// legacy formal path, remove prefix '/proxy'
				path = path.replace(LEGACY_FORMAL_PATH_PREFIX, "");
				needReGenConfigStr = true;
			}
		}

		if (!path.startsWith(TEST_PATH_PREFIX)) {
			// formal path add the custom gateway prefix
			path = String.format("%s%s", formalPathPrefix, path);
			needReGenConfigStr = true;
		}

		String resourceKey = method.toUpperCase() + ":" + path;
		String configId = cfgNode.select("$.id").getString();
		String configName = cfgNode.select("$.name").getString();
		long version = cfgNode.select("$.version").getLong();

		if (needReGenConfigStr) {
			cfgNode.set("path", path);
			configStr = cfgNode.toJson();
		}

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
		// config file entry ,if you want modify the aggregate config json but not use the interface of fizz,
		// you can just read the config ,transform to json format and modify it
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
		} else {

			String aggrMethodPath = null;
			try {
				for (Map.Entry<String, String> entry : aggregateResources.entrySet()) {
					aggrMethodPath = entry.getKey();
					boolean match = UrlTransformUtils.ANT_PATH_MATCHER.match(aggrMethodPath, key);
					if (match) {
						String configStr = aggregateResources.get(aggrMethodPath);
						Input input = createInput(configStr);
						Pipeline pipeline = createPipeline(configStr);
						if (pipeline != null && input != null) {
							Map<String, String> pathVariables = UrlTransformUtils.ANT_PATH_MATCHER.extractUriTemplateVariables(aggrMethodPath, key);
							Map<String, Object> map = Collections.emptyMap();
							if (!CollectionUtils.isEmpty(pathVariables)) {
								map = pathVariables.entrySet().stream().filter(
																	   		e -> {
																	   			return e.getKey().indexOf('$') == -1;
																	   		}
																	   )
																	   .collect(
																	   		Collectors.toMap(
																				Map.Entry::getKey,
																				e -> {
																					return (Object) e.getValue();
																				}
																	   		)
																	   );
							}
							com.fizzgate.util.ThreadContext.set("pathParams", map);
							return new AggregateResource(pipeline, input);
						} else {
							LOGGER.warn("request {} match {}, input {} pipeline {}", key, aggrMethodPath, input, pipeline);
							return null;
						}
					}
				}
			} catch (IOException e) {
				LOGGER.warn("request {} match {}, create input or pipeline error", key, aggrMethodPath, e);
				return null;
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

	private String extractServiceName(String path) {
		if (path != null) {
			if (path.startsWith(TEST_PATH_PREFIX)) {
				int endIndex = path.indexOf(FORWARD_SLASH, TEST_PATH_SERVICE_NAME_START_INDEX);
				if (endIndex > TEST_PATH_SERVICE_NAME_START_INDEX) {
					return path.substring(TEST_PATH_SERVICE_NAME_START_INDEX, endIndex);
				}
			} else if (path.startsWith(formalPathPrefix)) {
				int endIndex = path.indexOf(FORWARD_SLASH, formalPathServiceNameStartIndex);
				if (endIndex > formalPathServiceNameStartIndex) {
					return path.substring(formalPathServiceNameStartIndex, endIndex);
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
