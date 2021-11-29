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

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
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
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.dedicated_line.DedicatedLineService;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.FizzPluginFilterChain;
import we.plugin.requestbody.RequestBodyPlugin;
import we.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import we.spring.http.server.reactive.ext.FizzServerHttpResponseDecorator;
import we.util.Consts;
import we.util.NettyDataBufferUtils;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Francis Dong
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config) {
        String traceId = WebUtils.getTraceId(exchange);
        try {
            LogService.setBizId(traceId);
            String dedicatedLineId = WebUtils.getDedicatedLineId(exchange);
            String cryptoKey = dedicatedLineService.getRequestCryptoKey(dedicatedLineId);

            FizzServerHttpRequestDecorator request = (FizzServerHttpRequestDecorator) exchange.getRequest();
            return request.getBody().defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER).single().flatMap(body -> {
				if (body != NettyDataBufferUtils.EMPTY_DATA_BUFFER && systemConfig.fizzDedicatedLineClientRequestCrypto()) {
					byte[] bodyBytes = request.getBodyBytes();
					request.setBody(decrypt(bodyBytes, cryptoKey));
                    request.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
				}

                ServerHttpResponse original = exchange.getResponse();
                FizzServerHttpResponseDecorator fizzServerHttpResponseDecorator = new FizzServerHttpResponseDecorator(original) {
                    @Override
                    public Publisher<? extends DataBuffer> writeWith(DataBuffer remoteResponseBody) {
                        if (remoteResponseBody == null || remoteResponseBody == NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                            return Mono.empty();
                        } else {
                            if (StringUtils.isNotBlank(cryptoKey)) {
                                getDelegate().getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
                                byte[] bytes = remoteResponseBody.asByteBuffer().array();
                                NettyDataBuffer from = NettyDataBufferUtils.from(encrypt(bytes, cryptoKey));
                                return Mono.just(from);
                            } else {
                                return Mono.just(remoteResponseBody);
                            }
                        }
                    }
                };
                ServerWebExchange build = exchange.mutate().response(fizzServerHttpResponseDecorator).build();
                return FizzPluginFilterChain.next(build);
            });

        } catch (Exception e) {
            log.error("{} {} Exception", traceId, DEDICATED_LINE_CODEC_PLUGIN_FILTER, LogService.BIZ_ID, traceId, e);
            String respJson = WebUtils.jsonRespBody(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), traceId);
            return WebUtils.response(exchange, HttpStatus.INTERNAL_SERVER_ERROR, null, respJson);
        }
    }

    /*public String encrypt(String data, String secretKey) {
        if (StringUtils.isBlank(data)) {
            return data;
        }
        byte[] key = SecureUtil.decode(secretKey);
        SymmetricCrypto symmetric = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
        return symmetric.encryptBase64(data);
    }*/

    public byte[] encrypt(byte[] data, String secretKey) {
        byte[] key = SecureUtil.decode(secretKey);
        SymmetricCrypto symmetric = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
        return symmetric.encrypt(data);
    }

    /*public String decrypt(String data, String secretKey) {
        if (StringUtils.isBlank(data)) {
            return data;
        }
        byte[] key = SecureUtil.decode(secretKey);
        SymmetricCrypto symmetric = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
        return symmetric.decryptStr(data);
    }*/

    public byte[] decrypt(byte[] data, String secretKey) {
        byte[] key = SecureUtil.decode(secretKey);
        SymmetricCrypto symmetric = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
        return symmetric.decrypt(data);
    }
}
