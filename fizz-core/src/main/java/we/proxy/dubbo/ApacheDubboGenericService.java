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

package we.proxy.dubbo;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import reactor.core.publisher.Mono;
import we.fizz.exception.FizzException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author linwaiwai
 * @author Francis Dong
 *
 */
@Service
public class ApacheDubboGenericService {
	@Resource
	private ApacheDubboGenericServiceProperties apacheDubboGenericServiceProperties;

	@PostConstruct
	public void afterPropertiesSet() {

	}

	public ReferenceConfig<GenericService> createReferenceConfig(String serviceName, String version, String group) {
		ApplicationConfig applicationConfig = new ApplicationConfig();
		applicationConfig.setName("fizz_proxy");
		RegistryConfig registryConfig = new RegistryConfig();
		registryConfig.setAddress(apacheDubboGenericServiceProperties.getZookeeperAddress());
		ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
		referenceConfig.setInterface(serviceName);
		applicationConfig.setRegistry(registryConfig);
		referenceConfig.setApplication(applicationConfig);
		referenceConfig.setGeneric(true);
		referenceConfig.setAsync(true);
		referenceConfig.setTimeout(30000);
		referenceConfig.setVersion(version);
		referenceConfig.setGroup(group);
		applicationConfig.setQosEnable(false);
		return referenceConfig;
	}

	/**
	 * Generic invoke.
	 *
	 * @param body                 the json string body
	 * @param interfaceDeclaration the interface declaration
	 * @return the object
	 * @throws FizzException the fizz exception
	 */
	@SuppressWarnings("unchecked")
	public Mono<Object> send(final Map<String, Object> body, final DubboInterfaceDeclaration interfaceDeclaration,
			Map<String, String> attachments) {

		RpcContext.getContext().setAttachments(attachments);
		ReferenceConfig<GenericService> reference = createReferenceConfig(interfaceDeclaration.getServiceName(),
				interfaceDeclaration.getVersion(), interfaceDeclaration.getGroup());

		ReferenceConfigCache cache = ReferenceConfigCache.getCache();
		GenericService genericService = cache.get(reference);

		Pair<String[], Object[]> pair;
		if (CollectionUtils.isEmpty(body)) {
			pair = new ImmutablePair<String[], Object[]>(new String[] {}, new Object[] {});
		} else {
			pair = DubboUtils.parseDubboParam(body, interfaceDeclaration.getParameterTypes());
		}

		CompletableFuture<Object> future = null;
		Object object = genericService.$invoke(interfaceDeclaration.getMethod(), pair.getLeft(), pair.getRight());
		if (object == null) {
			future = RpcContext.getContext().getCompletableFuture();
		} else if (object instanceof CompletableFuture) {
			future = (CompletableFuture<Object>) object;
		} else {
			future = CompletableFuture.completedFuture(object);
		}
		Mono<Object> result = Mono.fromFuture(future.thenApply(ret -> {
			return ret;
		})).onErrorMap(exception -> exception instanceof GenericException
				? new FizzException(((GenericException) exception).getExceptionMessage())
				: new FizzException(exception));

		if (interfaceDeclaration.getTimeout() != null && interfaceDeclaration.getTimeout() > 0) {
			return result.timeout(Duration.ofMillis(interfaceDeclaration.getTimeout().longValue()));
		}
		return result;
	}

}
