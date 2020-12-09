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

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.listener.AggregateRedisConfig;
import we.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author hongqiaowei
 */

@Service
public class ApiConfigService {

    private static final Logger log = LoggerFactory.getLogger(ApiConfigService.class);

    private static final String signHeader           = "fizz-sign";

    private static final String timestampHeader      = "fizz-ts";

    private static final String secretKeyHeader      = "fizz-secretkey";

    @NacosValue(value = "${fizz-api-config.key:fizz_api_config_route}", autoRefreshed = true)
    @Value("${fizz-api-config.key:fizz_api_config_route}")
    private String fizzApiConfig;

    @NacosValue(value = "${fizz-api-config.channel:fizz_api_config_channel_route}", autoRefreshed = true)
    @Value("${fizz-api-config.channel:fizz_api_config_channel_route}")
    private String fizzApiConfigChannel;

    public  Map<String,  ServiceConfig> serviceConfigMap = new HashMap<>(128);

    private Map<Integer, ApiConfig>     apiConfigMap     = new HashMap<>(128);

    // TODO XXX
    @Value("${serviceWhiteList:x}")
    private String serviceWhiteList;
    private Set<String> whiteListSet = new HashSet<>(196);
    @ApolloConfigChangeListener
    private void configChangeListter(ConfigChangeEvent cce) {
        cce.changedKeys().forEach(
                k -> {
                    ConfigChange cc = cce.getChange(k);
                    if (cc.getPropertyName().equalsIgnoreCase("serviceWhiteList")) {
                        this.updateServiceWhiteList(cc.getOldValue(), cc.getNewValue());
                    }
                }
        );
    }

    private void updateServiceWhiteList(String oldValue, String newValue) {
        if (ObjectUtils.nullSafeEquals(oldValue, newValue)) {
            return;
        }
        log.info("old service white list: " + oldValue);
        serviceWhiteList = newValue;
        afterServiceWhiteListSet();
    }

    @NacosValue(value = "${serviceWhiteList:x}", autoRefreshed = true)
    public void setServiceWhiteList(String serviceWhiteList) {
        this.updateServiceWhiteList(this.serviceWhiteList, serviceWhiteList);
    }

    public void afterServiceWhiteListSet() {
        if (StringUtils.isNotBlank(serviceWhiteList)) {
            whiteListSet.clear();
            Arrays.stream(StringUtils.split(serviceWhiteList, Constants.Symbol.COMMA)).forEach(s -> {
                whiteListSet.add(s);
            });
            log.info("new service white list: " + whiteListSet.toString());
        } else {
            log.info("no service white list");
        }
    }

    @NacosValue(value = "${need-auth:false}", autoRefreshed = true)
    @Value("${need-auth:false}")
    private boolean needAuth;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private AppService appService;

    @Resource
    private GatewayGroupService gatewayGroupService;

    @Autowired(required = false)
    private CustomAuth customAuth;

    @NacosValue(value = "${openServiceWhiteList:false}", autoRefreshed = true)
    @Value("${openServiceWhiteList:false}")
    private boolean openServiceWhiteList = false;

    @PostConstruct
    public void init() throws Throwable {

        afterServiceWhiteListSet(); // TODO XXX

        final Throwable[] throwable = new Throwable[1];
        Throwable error = Mono.just(Objects.requireNonNull(rt.opsForHash().entries(fizzApiConfig)
                .defaultIfEmpty(new AbstractMap.SimpleEntry<>(ReactorUtils.OBJ, ReactorUtils.OBJ)).onErrorStop().doOnError(t -> {
                    log.info(null, t);
                })
                .concatMap(e -> {
                    Object k = e.getKey();
                    if (k == ReactorUtils.OBJ) {
                        return Flux.just(e);
                    }
                    Object v = e.getValue();
                    log.info(k.toString() + Constants.Symbol.COLON + v.toString(), LogService.BIZ_ID, k.toString());
                    String json = (String) v;
                    try {
                        ApiConfig ac = JacksonUtils.readValue(json, ApiConfig.class);
                        apiConfigMap.put(ac.id, ac);
                        updateServiceConfigMap(ac);
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
                    return lsnApiConfigChange();
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            throw error;
        }
    }

    public Mono<Throwable> lsnApiConfigChange() {
        final Throwable[] throwable = new Throwable[1];
        final boolean[] b = {false};
        rt.listenToChannel(fizzApiConfigChannel).doOnError(t -> {
            throwable[0] = t;
            b[0] = false;
            log.error("lsn " + fizzApiConfigChannel, t);
        }).doOnSubscribe(
                s -> {
                    b[0] = true;
                    log.info("success to lsn on " + fizzApiConfigChannel);
                }
        ).doOnNext(msg -> {
            String json = msg.getMessage();
            log.info(json, LogService.BIZ_ID, "acc" + System.currentTimeMillis());
            try {
                ApiConfig ac = JacksonUtils.readValue(json, ApiConfig.class);
                ApiConfig r = apiConfigMap.remove(ac.id);
                if (ac.isDeleted != ApiConfig.DELETED && r != null) {
                    r.isDeleted = ApiConfig.DELETED;
                    updateServiceConfigMap(r);
                }
                updateServiceConfigMap(ac);
                if (ac.isDeleted != ApiConfig.DELETED) {
                    apiConfigMap.put(ac.id, ac);
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

    private void updateServiceConfigMap(ApiConfig ac) {
        ServiceConfig sc = serviceConfigMap.get(ac.service);
        if (ac.isDeleted == ApiConfig.DELETED) {
            if (sc == null) {
                log.info("no " + ac.service + " config to delete");
            } else {
                sc.remove(ac);
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

        NO_SERVICE_CONFIG                 ("no service config"),

        NO_API_CONFIG                     ("no api config"),

        GATEWAY_GROUP_CANT_PROXY_API      ("gateway group cant proxy api"),

        APP_NOT_IN_API_LEGAL_APPS         ("app not in api legal apps"),

        IP_NOT_IN_WHITE_LIST              ("ip not in white list"),

        NO_TIMESTAMP_OR_SIGN              ("no timestamp or sign"),

        SIGN_INVALID                      ("sign invalid"),

        NO_CUSTOM_AUTH                    ("no custom auth"),

        CUSTOM_AUTH_REJECT                ("custom auth reject"),

        SERVICE_NOT_OPEN                  ("service not open"),

        CANT_ACCESS_SERVICE_API           ("cant access service api");

        private String reason;

        Access(String r) {
            reason = r;
        }

        public String getReason() {
            return reason;
        }
    }

    public Mono<Object> canAccess(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        HttpHeaders hdrs = req.getHeaders();
        LogService.setBizId(req.getId());
        return canAccess(exchange, WebUtils.getAppId(exchange),     WebUtils.getOriginIp(exchange), hdrs.getFirst(timestampHeader), hdrs.getFirst(signHeader), hdrs.getFirst(secretKeyHeader),
                                   WebUtils.getServiceId(exchange), req.getMethod(),                WebUtils.getReqPath(exchange));
    }

    private Mono<Object> canAccess(ServerWebExchange exchange, String     app,    String ip, String timestamp, String sign, String secretKey,
                                              String service,  HttpMethod method, String path) {

        if (openServiceWhiteList) {
            if (!whiteListSet.contains(service)) { // TODO XXX
                return Mono.just(Access.SERVICE_NOT_OPEN);
            }
        }
        ServiceConfig sc = serviceConfigMap.get(service);
        if (sc == null) {
            if (!needAuth) {
                return Mono.just(Access.YES);
            } else {
                return logWarnAndResult(service + Constants.Symbol.BLANK + Access.NO_SERVICE_CONFIG.getReason(), Access.NO_SERVICE_CONFIG);
            }
        } else {
            String api = ThreadContext.getStringBuilder().append(service).append(Constants.Symbol.BLANK).append(method.name()).append(Constants.Symbol.BLANK + path).toString();
            ApiConfig ac0 = null;
            for (String g : gatewayGroupService.currentGatewayGroupSet) { // compatible
                ac0 = sc.getApiConfig(method, path, g, app);
                if (ac0 != null) {
                    break;
                }
            }
            ApiConfig ac = ac0;
            if (ac == null) {
                    if (!needAuth) {
                        return Mono.just(Access.YES);
                    } else {
                        return logWarnAndResult(api + " no api config", Access.NO_API_CONFIG);
                    }
            } else if (gatewayGroupService.currentGatewayGroupIn(ac.gatewayGroups)) {
                    if (ac.apps.contains(App.ALL_APP)) {
                            return allow(api, ac);
                    } else if (app != null && ac.apps.contains(app)) {
                            if (ac.access == ApiConfig.ALLOW) {
                                    App a = appService.getApp(app);
                                    if (a.useWhiteList && !a.allow(ip)) {
                                        return logWarnAndResult(ip + " not in " + app + " white list", Access.IP_NOT_IN_WHITE_LIST);
                                    } else if (a.useAuth) {
                                        if (a.authType == App.SIGN_AUTH) {
                                            if (StringUtils.isBlank(timestamp) || StringUtils.isBlank(sign)) {
                                                return logWarnAndResult(app + " lack timestamp " + timestamp + " or sign " + sign, Access.NO_TIMESTAMP_OR_SIGN);
                                            } else if (!validate(app, timestamp, a.secretkey, sign)) {
                                                return logWarnAndResult(app + " sign " + sign + " invalid", Access.SIGN_INVALID);
                                            } else {
                                                return Mono.just(ac);
                                            }
                                        } else if (customAuth == null) {
                                            return logWarnAndResult(app + " no custom auth", Access.NO_CUSTOM_AUTH);
                                        } else {
                                            return customAuth.auth(exchange, app, ip, timestamp, sign, secretKey, a).flatMap(v -> {
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
                                    return logWarnAndResult("cant access " + api, Access.CANT_ACCESS_SERVICE_API);
                            }
                    } else {
                            return logWarnAndResult(app + " not in " + api + " legal apps", Access.APP_NOT_IN_API_LEGAL_APPS);
                    }
            } else {
                    return logWarnAndResult(gatewayGroupService.currentGatewayGroupSet + " cant proxy " + api, Access.GATEWAY_GROUP_CANT_PROXY_API);
            }
        }
    }

    private static Mono<Object> allow(String api, ApiConfig ac) {
        if (ac.access == ApiConfig.ALLOW) {
            return Mono.just(ac);
        } else {
            return logWarnAndResult("cant access " + api, Access.CANT_ACCESS_SERVICE_API);
        }
    }

    private static Mono logWarnAndResult(String msg, Access access) {
        log.warn(msg);
        return Mono.just(access);
    }

    private static boolean validate(String app, String timestamp, String secretKey, String sign) {
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(app).append(Constants.Symbol.UNDERLINE).append(timestamp).append(Constants.Symbol.UNDERLINE).append(secretKey);
        return sign.equalsIgnoreCase(DigestUtils.md532(b.toString()));
    }
}
