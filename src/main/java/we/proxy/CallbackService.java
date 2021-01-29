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

package we.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.fizz.AggregateResult;
import we.fizz.AggregateService;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.CallbackConfig;
import we.plugin.auth.Receiver;
import we.util.Constants;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author hongqiaowei
 */

@Service
public class CallbackService {

	private static final Logger log = LoggerFactory.getLogger(CallbackService.class);

	@Resource
	private FizzWebClient fizzWebClient;

	@Resource
	private AggregateService aggregateService;

	public Mono<? extends Void> requestBackends(ServerWebExchange exchange, HttpHeaders headers, Object body, CallbackConfig cc, HashMap<String, ServiceInstance> service2instMap) {
		ServerHttpRequest req = exchange.getRequest();
		int rs = cc.receivers.size();
		Mono<Object>[] monos = new Mono[rs];
		for (int i = 0; i < rs; i++) {
			Receiver r = cc.receivers.get(i);
			Mono send;
			if (r.type == ApiConfig.Type.SERVICE_DISCOVERY) {
				ServiceInstance si = service2instMap.get(r.service);
				if (si == null) {
					send = fizzWebClient.proxySend2service(req.getId(), req.getMethod(), r.service, r.path, headers, body);
				} else {
					String uri = buildUri(req, si, r.path);
					send = fizzWebClient.send(req.getId(), req.getMethod(), uri, headers, body);
				}
			} else {
				if (body instanceof DataBuffer) {
					send = aggregateService.request(exchange, r.service, r.path, headers, (DataBuffer) body);
				} else if (body instanceof String) {
					send = aggregateService.request(exchange, r.service, r.path, headers, (String) body);
				} else {
					return Mono.error(new RuntimeException("cant handle " + body, null, false, false) {});
				}
			}
			monos[i] = send;
		}
		return Flux.mergeSequential(monos)
				.reduce(
						new ArrayList<Object>(rs),
						(respCollector, resp) -> {
							respCollector.add(resp);
							return respCollector;
						}
				)
				.flatMap(
						resps -> {
							Object r = null;
							for (int i = 1; i < resps.size(); i++) {
								r = resps.get(i);
								if (r instanceof ClientResponse) {
									cleanup((ClientResponse) r);
								}
							}
							r = resps.get(0);
							if (r instanceof ClientResponse) {
								ClientResponse remoteResp = (ClientResponse) r;
								return genServerResponse(exchange, remoteResp);
							} else if (r instanceof AggregateResult) {
								AggregateResult ar = (AggregateResult) r;
								return aggregateService.genAggregateResponse(exchange, ar);
							} else {
								return Mono.error(new RuntimeException("cant response client with " + r, null, false, false) {});
							}
						}
				)
				;
	}

	private String buildUri(ServerHttpRequest req, ServiceInstance si, String path) {
		StringBuilder b = ThreadContext.getStringBuilder();
		b.append(req.getURI().getScheme())  .append(Constants.Symbol.COLON)  .append(Constants.Symbol.FORWARD_SLASH)  .append(Constants.Symbol.FORWARD_SLASH);
		b.append(si.ip)                     .append(Constants.Symbol.COLON)  .append(si.port)                         .append(path);
		return b.toString();
	}

	private Mono<? extends Void> genServerResponse(ServerWebExchange exchange, ClientResponse remoteResp) {
		ServerHttpResponse clientResp = exchange.getResponse();
		clientResp.setStatusCode(remoteResp.statusCode());
		HttpHeaders clientRespHeaders = clientResp.getHeaders();
		HttpHeaders remoteRespHeaders = remoteResp.headers().asHttpHeaders();
		remoteRespHeaders.entrySet().forEach(
				h -> {
					String k = h.getKey();
					if (clientRespHeaders.containsKey(k)) {
						if (k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)
								|| k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS) || k.equals(HttpHeaders.ACCESS_CONTROL_MAX_AGE)) {
						} else {
							clientRespHeaders.put(k, h.getValue());
						}
					} else {
						clientRespHeaders.put(k, h.getValue());
					}
				}
		);
		if (log.isDebugEnabled()) {
			StringBuilder b = ThreadContext.getStringBuilder();
			String rid = exchange.getRequest().getId();
			WebUtils.response2stringBuilder(rid, remoteResp, b);
			log.debug(b.toString(), LogService.BIZ_ID, rid);
		}
		return clientResp.writeWith(remoteResp.body(BodyExtractors.toDataBuffers()))
				.doOnError(throwable -> cleanup(remoteResp)).doOnCancel(() -> cleanup(remoteResp));
	}

	private void cleanup(ClientResponse clientResponse) {
		if (clientResponse != null) {
			clientResponse.bodyToMono(Void.class).subscribe();
		}
	}

}
