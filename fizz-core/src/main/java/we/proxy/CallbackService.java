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
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.constants.CommonConstants;
import we.fizz.AggregateResult;
import we.fizz.AggregateService;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.CallbackConfig;
import we.plugin.auth.Receiver;
import we.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author hongqiaowei
 */

@Service
public class CallbackService {

	private static final Logger          log      = LoggerFactory.getLogger(CallbackService.class);

	private static final String          callback = "callback";

	@Resource
	private FizzWebClient    fizzWebClient;

	@Resource
	private AggregateService aggregateService;

	@Resource
	private ApiConfigService apiConfigService;

	@Resource
	private SystemConfig     systemConfig;

	private String           aggrConfigPrefix;

	@PostConstruct
	public void postConstruct() {
		aggrConfigPrefix = systemConfig.getGatewayPrefix() + '/';
	}

	public Mono<Void> requestBackends(ServerWebExchange exchange, HttpHeaders headers, DataBuffer body, CallbackConfig cc, Map<String, ServiceInstance> service2instMap) {
		ServerHttpRequest req = exchange.getRequest();
		String traceId = WebUtils.getTraceId(exchange);
		HttpMethod method = req.getMethod();
		if (log.isDebugEnabled()) {
			log.debug(traceId + " service2instMap: " + JacksonUtils.writeValueAsString(service2instMap), LogService.BIZ_ID, traceId);
		}
		int rs = cc.receivers.size();
		Mono<Object>[] sends = new Mono[rs];
		for (int i = 0; i < rs; i++) {
			Receiver r = cc.receivers.get(i);
			Mono send;
			if (r.type == ApiConfig.Type.SERVICE_DISCOVERY) {
				ServiceInstance si = service2instMap.get(r.service);
//				if (si == null) {
//					send = fizzWebClient.send2service(traceId, method, r.service, r.path, headers, body)
//							            .onErrorResume(	crError(exchange, r, method, headers, body) );
//				} else {
					String uri = buildUri(req, si, r.path);
					send = fizzWebClient.send(traceId, method, uri, headers, body)
							            .onErrorResume( crError(exchange, r, method, headers, body)	);
//				}
			} else {
				send = aggregateService.request(WebUtils.getTraceId(exchange), WebUtils.getClientReqPathPrefix(exchange), method.name(), r.service, r.path, req.getQueryParams(), headers, body)
						               .onErrorResume( arError(exchange, r, method, headers, body) );
			}
			sends[i] = send;
		}
		return Flux.mergeSequential(sends)
				   .collectList()
				   .flatMap(
				   		sendResults -> {
				   			Object r = null;
				   			for (int i = 1; i < sendResults.size(); i++) {
				   				r = sendResults.get(i);
				   				if (r instanceof ClientResponse && !(r instanceof FizzFailClientResponse)) {
				   					clean((ClientResponse) r);
				   				}
				   			}
				   			r = sendResults.get(0);
				   			Throwable t = null;
				   			if (r instanceof FizzFailClientResponse) {
				   				t = ((FizzFailClientResponse) r).throwable;
				   				return Mono.error(Utils.runtimeExceptionWithoutStack(t.getMessage()));
				   			} if (r instanceof FailAggregateResult) {
				   				t = ((FailAggregateResult) r).throwable;
				   				return Mono.error(Utils.runtimeExceptionWithoutStack(t.getMessage()));
				   			} else if (r instanceof ClientResponse) {
				   				return genServerResponse(exchange, (ClientResponse) r);
				   			} else {
				   				return aggregateService.genAggregateResponse(exchange, (AggregateResult) r);
				   			}
				   		}
				   )
				   ;
	}

	private Function<Throwable, Mono<? extends ClientResponse>> crError(ServerWebExchange exchange, Receiver r, HttpMethod method, HttpHeaders headers, DataBuffer body) {
		return t -> {
			log(exchange, r, method, headers, body, t);
			return Mono.just(new FizzFailClientResponse(t));
		};
	}

	private Function<Throwable, Mono<AggregateResult>> arError(ServerWebExchange exchange, Receiver r, HttpMethod method, HttpHeaders headers, DataBuffer body) {
		return t -> {
			log(exchange, r, method, headers, body, t);
			return Mono.just(new FailAggregateResult(t));
		};
	}

	private void log(ServerWebExchange exchange, Receiver r, HttpMethod method, HttpHeaders headers, DataBuffer body, Throwable t) {
		StringBuilder b = ThreadContext.getStringBuilder();
		WebUtils.request2stringBuilder(exchange, b);
		b.append(Consts.S.LINE_SEPARATOR).append(callback).append(Consts.S.LINE_SEPARATOR);
		String traceId = WebUtils.getTraceId(exchange);
		WebUtils.request2stringBuilder(traceId, method, r.service + Consts.S.FORWARD_SLASH + r.path, headers, body, b);
		log.error(b.toString(), LogService.BIZ_ID, traceId, t);
	}

	private String buildUri(ServerHttpRequest req, ServiceInstance si, String path) {
		StringBuilder b = ThreadContext.getStringBuilder();
		b.append(req.getURI().getScheme())  .append(Consts.S.COLON)  .append(Consts.S.FORWARD_SLASH)  .append(Consts.S.FORWARD_SLASH);
		b.append(si.ip)                     .append(Consts.S.COLON)  .append(si.port)                 .append(path);
		return b.toString();
	}

	private String buildUri(String scheme, ServiceInstance si, String path) {
		StringBuilder b = ThreadContext.getStringBuilder();
		b.append(scheme)                    .append(Consts.S.COLON)  .append(Consts.S.FORWARD_SLASH)  .append(Consts.S.FORWARD_SLASH);
		b.append(si.ip)                     .append(Consts.S.COLON)  .append(si.port)                 .append(path);
		return b.toString();
	}

	private Mono<Void> genServerResponse(ServerWebExchange exchange, ClientResponse remoteResp) {
		ServerHttpResponse clientResp = exchange.getResponse();
		clientResp.setStatusCode(remoteResp.statusCode());
		HttpHeaders clientRespHeaders = clientResp.getHeaders();
		HttpHeaders remoteRespHeaders = remoteResp.headers().asHttpHeaders();
		remoteRespHeaders.entrySet().forEach(
				h -> {
					String k = h.getKey();
					if (clientRespHeaders.containsKey(k)) {
						if (k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)
								                                              || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
																			  || k.equals(HttpHeaders.ACCESS_CONTROL_MAX_AGE)
																			  || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)) {
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
			String traceId = WebUtils.getTraceId(exchange);
			WebUtils.response2stringBuilder(traceId, remoteResp, b);
			log.debug(b.toString(), LogService.BIZ_ID, traceId);
		}
		return clientResp.writeWith(remoteResp.body(BodyExtractors.toDataBuffers()))
				         .doOnError(throwable -> clean(remoteResp)).doOnCancel(() -> clean(remoteResp));
	}

	public Mono<Result> replay(CallbackReplayReq req) {

		HashSet<String> gatewayGroups = new HashSet<>();
		gatewayGroups.add(req.gatewayGroup);
		Result<ApiConfig> result = apiConfigService.get(false, gatewayGroups, req.app, req.service, req.method, req.path);
		ApiConfig ac = result.data;
		if (ac == null) {
			return Mono.just(Result.fail("no api config for " + req.path));
		}
		CallbackConfig cc = ac.callbackConfig;

		List<Mono<Object>> sends = new ArrayList<>(); Mono send;

		if (req.replayType == CallbackReplayReq.Type.ORIGINAL_PATH) {

			int rs = cc.receivers.size();
			for (int i = 0; i < rs; i++) {
				Receiver r = cc.receivers.get(i);
				if (r.type == ApiConfig.Type.SERVICE_DISCOVERY) {
					ServiceInstance si = req.receivers.get(r.service);
					if (si != null) {
						String uri = buildUri("http", si, r.path);
						send = fizzWebClient.send(req.id, req.method, uri, req.headers, req.body)
								            .onErrorResume( crError(req, r.service, r.path) );
						sends.add(send);
					}
				} else {
					String traceId = CommonConstants.TRACE_ID_PREFIX + req.id;
					send = aggregateService.request(traceId, aggrConfigPrefix, req.method.name(), r.service, r.path, null, req.headers, req.body)
							               .onErrorResume( arError(req, r.service, r.path) );
					sends.add(send);
				}
			}

		} else {

			for (ServiceTypePath stp : req.assignServices) {
				if (stp.type == ApiConfig.Type.SERVICE_DISCOVERY) {
					String svc = null;
					if (stp.registryCenter == null) {
						svc = stp.service;
					} else {
						svc = stp.registryCenter + Consts.S.COMMA + stp.service;
					}
					send = fizzWebClient.send2service(req.id, req.method, svc, stp.path, req.headers, req.body)
							            .onErrorResume( crError(req, stp.service, stp.path) );
				} else {
					String traceId = CommonConstants.TRACE_ID_PREFIX + req.id;
					send = aggregateService.request(traceId, aggrConfigPrefix, req.method.name(), stp.service, stp.path, null, req.headers, req.body)
							               .onErrorResume( arError(req, stp.service, stp.path) );
				}
				sends.add(send);
			}
		}

		int ss = sends.size();
		Mono<Object>[] sendArr = sends.toArray(new Mono[ss]);
		return Flux.mergeSequential(sendArr)
				   .collectList()
				   .map(
				   		sendResults -> {
				   			int c = Result.SUCC;
				   			Throwable t = null;
				   			for (int i = 0; i < sendResults.size(); i++) {
				   				Object r = sendResults.get(i);
				   				if (r instanceof FizzFailClientResponse) {
				   					c = Result.FAIL;
				   					t = ((FizzFailClientResponse) r).throwable;
				   				} else if (r instanceof FailAggregateResult) {
				   					c = Result.FAIL;
				   					t = ((FailAggregateResult) r).throwable;
				   				} else if (r instanceof ClientResponse) {
				   					clean((ClientResponse) r);
				   				}
				   			}
				   			return Result.with(c, t);
				   		}
				   )
				   ;
	}

	private Function<Throwable, Mono<? extends AggregateResult>> arError(CallbackReplayReq req, String service, String path) {
		return t -> {
			log(req, service, path, t);
			return Mono.just(new FailAggregateResult(t));
		};
	}

	private Function<Throwable, Mono<? extends ClientResponse>> crError(CallbackReplayReq req, String service, String path) {
		return t -> {
			log(req, service, path, t);
			return Mono.just(new FizzFailClientResponse(t));
		};
	}

	private void log(CallbackReplayReq req, String service, String path, Throwable t) {
		StringBuilder b = ThreadContext.getStringBuilder();
		b.append(req.service).append(Consts.S.FORWARD_SLASH).append(req.path);
		b.append(Consts.S.LINE_SEPARATOR).append(callback).append(Consts.S.LINE_SEPARATOR);
		WebUtils.request2stringBuilder(req.id, req.method, service + Consts.S.FORWARD_SLASH + path, req.headers, req.body, b);
		log.error(b.toString(), LogService.BIZ_ID, req.id, t);
	}

	private void clean(ClientResponse cr) {
		cr.bodyToMono(Void.class).subscribe();
	}
}
