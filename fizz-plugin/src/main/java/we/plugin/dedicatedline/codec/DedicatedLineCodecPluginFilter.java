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

package we.plugin.dedicatedline.codec;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.dedicated_line.DedicatedLineService;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.FizzPluginFilterChain;
import we.plugin.auth.App;
import we.plugin.auth.AppService;
import we.plugin.requestbody.RequestBodyPlugin;
import we.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import we.spring.http.server.reactive.ext.FizzServerHttpResponseDecorator;
import we.util.NettyDataBufferUtils;
import we.util.WebUtils;

/**
 * 
 * @author Francis Dong
 *
 */
@ConditionalOnBean(DedicatedLineService.class)
@Component(DedicatedLineCodecPluginFilter.DEDICATED_LINE_CODEC_PLUGIN_FILTER)
public class DedicatedLineCodecPluginFilter extends RequestBodyPlugin {

	private static final Logger log = LoggerFactory.getLogger(DedicatedLineCodecPluginFilter.class);

	public static final String DEDICATED_LINE_CODEC_PLUGIN_FILTER = "dedicatedLineCodecPlugin";

	@Resource
	private SystemConfig systemConfig;

	@Resource
	private DedicatedLineService dedicatedLineService;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config) {
		String traceId = WebUtils.getTraceId(exchange);
		try {
			LogService.setBizId(traceId);
			String dedicatedLineId = WebUtils.getDedicatedLineId(exchange);
			String secretKey = dedicatedLineService.getPairCodeSecretKey(dedicatedLineId);

			FizzServerHttpRequestDecorator request = (FizzServerHttpRequestDecorator) exchange.getRequest();
			return request.getBody().defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER).single().flatMap(body -> {
				String reqBody = body.toString(StandardCharsets.UTF_8);
				request.setBody(decrypt(reqBody, secretKey));

				ServerHttpResponse original = exchange.getResponse();
				FizzServerHttpResponseDecorator fizzServerHttpResponseDecorator = new FizzServerHttpResponseDecorator(
						original) {
					@Override
					public Publisher<? extends DataBuffer> writeWith(DataBuffer remoteResponseBody) {
						String respBody = remoteResponseBody.toString(StandardCharsets.UTF_8);
						HttpHeaders headers = getDelegate().getHeaders();
						headers.setContentType(MediaType.TEXT_PLAIN);
						headers.remove(HttpHeaders.CONTENT_LENGTH);
						NettyDataBuffer from = NettyDataBufferUtils.from(encrypt(respBody, secretKey));
						return Mono.just(from);
					}
				};
				ServerWebExchange build = exchange.mutate().response(fizzServerHttpResponseDecorator).build();
				return FizzPluginFilterChain.next(build);
			});

		} catch (Exception e) {
			log.error("{} {} Exception", traceId, DEDICATED_LINE_CODEC_PLUGIN_FILTER, e, LogService.BIZ_ID, traceId);
			String respJson = WebUtils.jsonRespBody(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), traceId);
			return WebUtils.response(exchange, HttpStatus.INTERNAL_SERVER_ERROR, null, respJson);
		}
	}

	public String encrypt(String data, String secretKey) {
		if (StringUtils.isBlank(data)) {
			return data;
		}
		byte[] key = SecureUtil.decode(secretKey);
		SymmetricCrypto symmetric = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
		return symmetric.encryptBase64(data);
	}

	public String decrypt(String data, String secretKey) {
		if (StringUtils.isBlank(data)) {
			return data;
		}
		byte[] key = SecureUtil.decode(secretKey);
		SymmetricCrypto symmetric = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
		return symmetric.decryptStr(data);
	}

}
