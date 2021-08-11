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

package we.plugin.auth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author hongqiaowei
 */

@Service
public class ApiConfigService {

    private static final Logger log      = LoggerFactory.getLogger(ApiConfigService.class);

    private static final String mpps     = "$mpps";

    private static final String deny     = "route which match current request can't be access by client app, or is not exposed to current gateway group";

    public  static final String AUTH_MSG = "$authMsg";

    public  Map<String,  ServiceConfig> serviceConfigMap = new HashMap<>(128);

    private Map<Integer, ApiConfig>     apiConfigMap     = new HashMap<>(128);

    @Resource
    private ApiConfigServiceProperties apiConfigServiceProperties;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private AppService appService;

    @Resource
    private ApiConifg2appsService apiConifg2appsService;

    @Resource
    private GatewayGroupService gatewayGroupService;

    @Resource
    private SystemConfig systemConfig;

    @Autowired(required = false)
    private CustomAuth customAuth;

    @PostConstruct
    public void init() throws Throwable {
        this.init(this::lsnApiConfigChange);
    }

    public void refreshLocalCache() throws Throwable {
        this.init(null);
    }

    private void init(Supplier<Mono<Throwable>> doAfterLoadCache) throws Throwable {
        Map<Integer, ApiConfig> apiConfigMapTmp = new HashMap<>(128);
        Map<String,  ServiceConfig> serviceConfigMapTmp = new HashMap<>(128);
        final Throwable[] throwable = new Throwable[1];
        Throwable error = Mono.just(Objects.requireNonNull(rt.opsForHash().entries(apiConfigServiceProperties.getFizzApiConfig())
                .defaultIfEmpty(new AbstractMap.SimpleEntry<>(ReactorUtils.OBJ, ReactorUtils.OBJ)).onErrorStop().doOnError(t -> log.info(null, t))
                .concatMap(e -> {
                    Object k = e.getKey();
                    if (k == ReactorUtils.OBJ) {
                        return Flux.just(e);
                    }
                    Object v = e.getValue();
                    log.info("api config: " + v.toString(), LogService.BIZ_ID, k.toString());
                    String json = (String) v;
                    try {
                        ApiConfig ac = JacksonUtils.readValue(json, ApiConfig.class);
                        apiConfigMapTmp.put(ac.id, ac);
                        updateServiceConfigMap(ac, serviceConfigMapTmp);
                        return Flux.just(e);
                    } catch (Throwable t) {
                        throwable[0] = t;
                        log.info(json, t);
                        return Flux.error(t);
                    }
                }).blockLast())).flatMap(
                e -> {
                    if (throwable[0] != null) {
                        return Mono.error(throwable[0]);
                    }

                    if (doAfterLoadCache != null) {
                        return doAfterLoadCache.get();
                    } else {
                        return Mono.just(ReactorUtils.EMPTY_THROWABLE);
                    }
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            throw error;
        }

        this.apiConfigMap = apiConfigMapTmp;
        this.serviceConfigMap = serviceConfigMapTmp;
    }

    private Mono<Throwable> lsnApiConfigChange() {
        final Throwable[] throwable = new Throwable[1];
        final boolean[] b = {false};
        rt.listenToChannel(apiConfigServiceProperties.getFizzApiConfigChannel()).doOnError(t -> {
            throwable[0] = t;
            b[0] = false;
            log.error("lsn " + apiConfigServiceProperties.getFizzApiConfigChannel(), t);
        }).doOnSubscribe(
                s -> {
                    b[0] = true;
                    log.info("success to lsn on " + apiConfigServiceProperties.getFizzApiConfigChannel());
                }
        ).doOnNext(msg -> {
            String json = msg.getMessage();
            log.info(json, LogService.BIZ_ID, "acc" + System.currentTimeMillis());
            try {
                ApiConfig ac = JacksonUtils.readValue(json, ApiConfig.class);
                ApiConfig r = apiConfigMap.remove(ac.id);
                if (ac.isDeleted != ApiConfig.DELETED && r != null) {
                    r.isDeleted = ApiConfig.DELETED;
                    updateServiceConfigMap(r, serviceConfigMap);
                }
                updateServiceConfigMap(ac, serviceConfigMap);
                if (ac.isDeleted != ApiConfig.DELETED) {
                    apiConfigMap.put(ac.id, ac);
                } else {
                    apiConifg2appsService.remove(ac.id);
                }
            } catch (Throwable t) {
                log.info(json, t);
            }
        }).subscribe();
        Throwable t = throwable[0];
        while (!b[0]) {
            if (t != null) {
                return Mono.error(t);
            } else {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    return Mono.error(e);
                }
            }
        }
        return Mono.just(ReactorUtils.EMPTY_THROWABLE);
    }

    private void updateServiceConfigMap(ApiConfig ac, Map<String, ServiceConfig> serviceConfigMap) {
        ServiceConfig sc = serviceConfigMap.get(ac.service);
        if (ac.isDeleted == ApiConfig.DELETED) {
            if (sc == null) {
                log.info("no " + ac.service + " config to delete");
            } else {
                sc.remove(ac);
                if (sc.path2methodToApiConfigMapMap.isEmpty()) {
                    serviceConfigMap.remove(ac.service);
                }
                // apiConifg2appsService.remove(ac.id);
            }
        } else {
            if (sc == null) {
                sc = new ServiceConfig(ac.service);
                serviceConfigMap.put(ac.service, sc);
                sc.add(ac);
            } else {
                sc.update(ac);
            }
        }
    }

    public enum Access {

        YES                               (null),

        IP_NOT_IN_WHITE_LIST              ("ip not in white list"),

        NO_TIMESTAMP_OR_SIGN              ("no timestamp or sign"),

        NO_SECRETKEY                      ("no secretkey"),

        SIGN_INVALID                      ("sign invalid"),

        SECRETKEY_INVALID                 ("secretkey invalid"),

        NO_CUSTOM_AUTH                    ("no custom auth"),

        CUSTOM_AUTH_REJECT                ("custom auth reject");

        private String reason;

        Access(String r) {
            reason = r;
        }

        public String getReason() {
            return reason;
        }
    }

    public ApiConfig getApiConfig(String app, String service, HttpMethod method, String path) {
        ApiConfig ac = null;
        for (String g : gatewayGroupService.currentGatewayGroupSet) {
            ac = getApiConfig(service, method, path, g, app);
            if (ac != null) {
                return ac;
            }
        }
        return ac;
    }

    public ApiConfig getApiConfig(String service, HttpMethod method, String path, String gatewayGroup, String app) {
        ServiceConfig sc = serviceConfigMap.get(service);
        if (sc != null) {
            List<ApiConfig> apiConfigs = sc.getApiConfigs(method, path, gatewayGroup);
            if (!apiConfigs.isEmpty()) {
                List<String> matchPathPatterns = ThreadContext.getArrayList(mpps, String.class);
                for (int i = 0; i < apiConfigs.size(); i++) {
                    ApiConfig ac = apiConfigs.get(i);
                    if (ac.checkApp) {
                        if (apiConifg2appsService.contains(ac.id, app)) {
                            matchPathPatterns.add(ac.path);
                        } /*else {
                            if (app == null) {
                                ThreadContext.set(ApiConfigService.AUTH_MSG, "request not carry app message");
                            } else {
                                ThreadContext.set(ApiConfigService.AUTH_MSG, app + " can't access " + service + ' ' + method + ' ' + path);
                            }
                        }*/
                    } else {
                        matchPathPatterns.add(ac.path);
                    }
                }
                if (!matchPathPatterns.isEmpty()) {
                    if (matchPathPatterns.size() > 1) {
                        Collections.sort(matchPathPatterns, UrlTransformUtils.ANT_PATH_MATCHER.getPatternComparator(path));
                    }
                    String bestPathPattern = matchPathPatterns.get(0);
                    for (int i = 0; i < apiConfigs.size(); i++) {
                        ApiConfig ac = apiConfigs.get(i);
                        if (StringUtils.equals(ac.path, bestPathPattern)) {
                            return ac;
                        }
                    }
                }
            }
        } else {
            ThreadContext.set(ApiConfigService.AUTH_MSG, "no " + service + " service config");
        }
        return null;
    }

    public Mono<Object> canAccess(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        HttpHeaders hdrs = req.getHeaders();
        LogService.setBizId(req.getId());
        return canAccess(exchange, WebUtils.getAppId(exchange),         WebUtils.getOriginIp(exchange), getTimestamp(hdrs),                     getSign(hdrs),
                                   WebUtils.getClientService(exchange), req.getMethod(),                WebUtils.getClientReqPath(exchange));
    }

    // TODO: improve ...
    private Mono<Object> canAccess(ServerWebExchange exchange, String app, String ip, String timestamp, String sign, String service, HttpMethod method, String path) {

        if (!systemConfig.isAggregateTestAuth()) {
            if (SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX0.equals(WebUtils.getClientReqPathPrefix(exchange))) {
                return Mono.just(Access.YES);
            }
        }

        ApiConfig ac = getApiConfig(app, service, method, path);
        if (ac == null) {
            String authMsg = (String) ThreadContext.remove(AUTH_MSG);
            if (authMsg == null) {
                authMsg = deny;
            }
            if (!apiConfigServiceProperties.isNeedAuth()) {
                return Mono.just(Access.YES);
            } else {
                return logAndResult(authMsg);
            }

        } else if (ac.checkApp) {
            App a = appService.getApp(app);
            if (a.useWhiteList && !a.allow(ip)) {
                return logAndResult(ip + " not in " + app + " white list", Access.IP_NOT_IN_WHITE_LIST);
            } else if (a.useAuth) {
                if (a.authType == App.AUTH_TYPE.SIGN) {
                    return authSign(ac, a, timestamp, sign);
                } else if (a.authType == App.AUTH_TYPE.SECRETKEY) {
                    return authSecretkey(ac, a, sign);
                } else if (customAuth == null) {
                    return logAndResult(app + " no custom auth", Access.NO_CUSTOM_AUTH);
                } else {
                    return customAuth.auth(exchange, app, ip, timestamp, sign, a).flatMap(v -> {
                        if (v == Access.YES) {
                            return Mono.just(ac);
                        } else {
                            return Mono.just(Access.CUSTOM_AUTH_REJECT);
                        }
                    });
                }
            } else {
                return Mono.just(ac);
            }

        } else {
            return Mono.just(ac);
        }
    }

    private Mono authSign(ApiConfig ac, App a, String timestamp, String sign) {
        if (StringUtils.isAnyBlank(timestamp, sign)) {
            return logAndResult(a.app + " lack timestamp " + timestamp + " or sign " + sign, Access.NO_TIMESTAMP_OR_SIGN);
        } else if (validate(a.app, timestamp, a.secretkey, sign)) {
            return Mono.just(ac);
        } else {
            return logAndResult(a.app + " sign " + sign + " invalid", Access.SIGN_INVALID);
        }
    }

    private boolean validate(String app, String timestamp, String secretKey, String sign) {
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(app).append(Constants.Symbol.UNDERLINE).append(timestamp).append(Constants.Symbol.UNDERLINE).append(secretKey);
        return sign.equalsIgnoreCase(DigestUtils.md532(b.toString()));
    }

    private Mono authSecretkey(ApiConfig ac, App a, String sign) {
        if (StringUtils.isBlank(sign)) {
            return logAndResult(a.app + " lack secretkey " + sign, Access.NO_SECRETKEY);
        } else if (a.secretkey.equals(sign)) {
            return Mono.just(ac);
        } else {
            return logAndResult(a.app + " secretkey " + sign + " invalid", Access.SECRETKEY_INVALID);
        }
    }

    private Mono logAndResult(String msg, Access access) {
        log.warn(msg);
        return Mono.just(access);
    }

    private Mono logAndResult(String msg) {
        log.warn(msg);
        return Mono.just(msg);
    }

    private String getTimestamp(HttpHeaders reqHdrs) {
        List<String> tsHdrs = systemConfig.getTimestampHeaders();
        for (int i = 0; i < tsHdrs.size(); i++) {
            String a = reqHdrs.getFirst(tsHdrs.get(i));
            if (a != null) {
                return a;
            }
        }
        return null;
    }

    private String getSign(HttpHeaders reqHdrs) {
        List<String> signHdrs = systemConfig.getSignHeaders();
        for (int i = 0; i < signHdrs.size(); i++) {
            String a = reqHdrs.getFirst(signHdrs.get(i));
            if (a != null) {
                return a;
            }
        }
        return null;
    }
}
