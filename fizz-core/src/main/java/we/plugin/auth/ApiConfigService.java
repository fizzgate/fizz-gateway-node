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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.Fizz;
import we.config.AggregateRedisConfig;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.FizzPluginFilter;
import we.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * @author hongqiaowei
 */

@Service
public class ApiConfigService {

    private static final Logger log      = LoggerFactory.getLogger(ApiConfigService.class);

//  private static final String macs     = "macsT";

    public  Map<String,  ServiceConfig> serviceConfigMap = new HashMap<>(128);

    private Map<Integer, ApiConfig>     apiConfigMap     = new HashMap<>(128);

    private Map<String,  String>        pluginConfigMap  = new HashMap<>(32);

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
        Result<?> result = initPlugin();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
        result = lsnPluginConfigChange();
        if (result.code == Result.FAIL) {
            throw new RuntimeException(result.msg, result.t);
        }
    }

    // TODO: no need like this
    public void refreshLocalCache() throws Throwable {
        this.init(null);
        initPlugin();
    }

    // TODO: no need like this
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
                    log.info("init api config: {}", v.toString(), LogService.BIZ_ID, k.toString());
                    String json = (String) v;
                    try {
                        ApiConfig ac = JacksonUtils.readValue(json, ApiConfig.class);
                        apiConfigMapTmp.put(ac.id, ac);
                        updateServiceConfigMap(ac, serviceConfigMapTmp);
                        return Flux.just(e);
                    } catch (Throwable t) {
                        throwable[0] = t;
                        log.error("deser {}", json, t);
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

    // TODO: no need like this
    private Mono<Throwable> lsnApiConfigChange() {
        final Throwable[] throwable = new Throwable[1];
        final boolean[] b = {false};
        String ch = apiConfigServiceProperties.getFizzApiConfigChannel();
        rt.listenToChannel(ch).doOnError(t -> {
            throwable[0] = t;
            b[0] = false;
            log.error("lsn {}", ch, t);
        }).doOnSubscribe(
            s -> {
                b[0] = true;
                log.info("success to lsn on {}", ch);
            }
        ).doOnNext(msg -> {
            String json = msg.getMessage();
            log.info("api config change: {}", json, LogService.BIZ_ID, "acc" + System.currentTimeMillis());
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
                log.error("deser {}", json, t);
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

    private Result<?> initPlugin() {
        Result<?> result = Result.succ();
        String key = apiConfigServiceProperties.getFizzPluginConfig();
        Flux<Map.Entry<Object, Object>> plugins = rt.opsForHash().entries(key);
        plugins.collectList()
               .defaultIfEmpty(Collections.emptyList())
               .flatMap(
                       es -> {
                           if (Fizz.context != null) {
                               String json = null;
                               try {
                                   for (Map.Entry<Object, Object> e : es) {
                                       json = (String) e.getValue();
                                       HashMap<?, ?> map = JacksonUtils.readValue(json, HashMap.class);
                                       String plugin = (String) map.get("plugin");
                                       String pluginConfig = (String) map.get("fixedConfig");
                                       String currentPluginConfig = pluginConfigMap.get(plugin);
                                       if (currentPluginConfig == null || !currentPluginConfig.equals(pluginConfig)) {
                                           if (Fizz.context.containsBean(plugin)) {
                                               FizzPluginFilter pluginFilter = (FizzPluginFilter) Fizz.context.getBean(plugin);
                                               pluginFilter.init(pluginConfig);
                                               pluginConfigMap.put(plugin, pluginConfig);
                                               log.info("init {} with {}", plugin, pluginConfig);
                                           } else {
                                               log.warn("no {} bean", plugin);
                                           }
                                       }
                                   }
                               } catch (Throwable t) {
                                   result.code = Result.FAIL;
                                   result.msg  = "init plugin error, config: " + json;
                                   result.t    = t;
                               }
                           }
                           return Mono.empty();
                       }
               )
               .onErrorReturn(
                       throwable -> {
                           result.code = Result.FAIL;
                           result.msg  = "init plugin error";
                           result.t    = throwable;
                           return true;
                       },
                       result
               )
               .block();
        return result;
    }

    private Result<?> lsnPluginConfigChange() {
        Result<?> result = Result.succ();
        String channel = apiConfigServiceProperties.getFizzPluginConfigChannel();
        rt.listenToChannel(channel)
          .doOnError(
                  t -> {
                      result.code = ReactiveResult.FAIL;
                      result.msg  = "lsn error, channel: " + channel;
                      result.t    = t;
                      log.error("lsn channel {} error", channel, t);
                  }
          )
          .doOnSubscribe(
                  s -> {
                      log.info("success to lsn on {}", channel);
                  }
          )
          .doOnNext(
                  msg -> {
                      if (Fizz.context != null) {
                          String message = msg.getMessage();
                          try {
                              HashMap<?, ?> map = JacksonUtils.readValue(message, HashMap.class);
                              String plugin = (String) map.get("plugin");
                              String pluginConfig = (String) map.get("fixedConfig");
                              String currentPluginConfig = pluginConfigMap.get(plugin);
                              if (currentPluginConfig == null || !currentPluginConfig.equals(pluginConfig)) {
                                  if (Fizz.context.containsBean(plugin)) {
                                      FizzPluginFilter pluginFilter = (FizzPluginFilter) Fizz.context.getBean(plugin);
                                      pluginFilter.init(pluginConfig);
                                      pluginConfigMap.put(plugin, pluginConfig);
                                      log.info("init {} with {} again", plugin, pluginConfig);
                                  } else {
                                      log.warn("no {} bean", plugin);
                                  }
                              }
                          } catch (Throwable t) {
                              log.error("message: {}", message, t);
                          }
                      }
                  }
          )
          .subscribe();
        return result;
    }

    private void updateServiceConfigMap(ApiConfig ac, Map<String, ServiceConfig> serviceConfigMap) {
        ServiceConfig sc = serviceConfigMap.get(ac.service);
        if (ac.isDeleted == ApiConfig.DELETED) {
            if (sc != null) {
                sc.remove(ac);
                if (sc.apiConfigMap.isEmpty()) {
                    serviceConfigMap.remove(ac.service);
                }
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

    /**
     * @deprecated
     */
    @Deprecated
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
        Result<ApiConfig> result = get(null, app, service, method, path);
        if (result.code == Result.SUCC) {
            return result.data;
        }
        return null;
    }

    public Result<ApiConfig> get(String app, String service, HttpMethod method, String path) {
        return get(null, app, service, method, path);
    }

    public ApiConfig getApiConfig(Set<String> gatewayGroups, String app, String service, HttpMethod method, String path) {
        Result<ApiConfig> result = get(null, app, service, method, path);
        if (result.code == Result.SUCC) {
            return result.data;
        }
        return null;
    }

    public Result<ApiConfig> get(Set<String> gatewayGroups, String app, String service, HttpMethod method, String path) {
        ServiceConfig sc = serviceConfigMap.get(service);
        if (sc == null) {
            return Result.fail("no " + service + " service api config");
        }
        if (CollectionUtils.isEmpty(gatewayGroups)) {
            gatewayGroups = gatewayGroupService.currentGatewayGroupSet;
        }
        List<ApiConfig> apiConfigs = sc.getApiConfigs(gatewayGroups, method, path);
        if (apiConfigs.isEmpty()) {
            StringBuilder b = ThreadContext.getStringBuilder();
            b.append(service).append(" don't have api config matching ").append(gatewayGroups).append(" group ").append(method).append(" method ").append(path).append(" path");
            return Result.fail(b.toString());
        }
//      List<ApiConfig> appCanAccess = ThreadContext.getArrayList(macs);
        List<ApiConfig> appCanAccess = ThreadContext.getArrayList();
        for (int i = 0; i < apiConfigs.size(); i++) {
            ApiConfig ac = apiConfigs.get(i);
            if (ac.checkApp) {
                if (StringUtils.isNotBlank(app) && apiConifg2appsService.contains(ac.id, app)) {
                    appCanAccess.add(ac);
                }
            } else {
                appCanAccess.add(ac);
            }
        }
        if (appCanAccess.isEmpty()) {
            StringBuilder b = ThreadContext.getStringBuilder();
            b.append("app ").append(app).append(" can't access ").append(JacksonUtils.writeValueAsString(apiConfigs));
            return Result.fail(b.toString());
        }
        ApiConfig bestOne = appCanAccess.get(0);
        if (appCanAccess.size() != 1) {
            appCanAccess.sort(new ApiConfigPathPatternComparator(path)); // singleton ?
            ApiConfig ac0 = appCanAccess.get(0);
            bestOne = ac0;
            ApiConfig ac1 = appCanAccess.get(1);
            if (ac0.path.equals(ac1.path)) {
                if (ac0.fizzMethod == ac1.fizzMethod) {
                    if (StringUtils.isNotBlank(app)) {
                        if (!ac0.checkApp) {
                            bestOne = ac1;
                        }
                    }
                } else {
                    if (ac0.fizzMethod == ApiConfig.ALL_METHOD) {
                        bestOne = ac1;
                    }
                }
            }
        }
        return Result.succ(bestOne);
    }

    public Mono<Result<ApiConfig>> auth(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        HttpHeaders hdrs = req.getHeaders();
        LogService.setBizId(WebUtils.getTraceId(exchange));
        return auth(exchange, WebUtils.getAppId(exchange),         WebUtils.getOriginIp(exchange), getTimestamp(hdrs),                     getSign(hdrs),
                              WebUtils.getClientService(exchange), req.getMethod(),                WebUtils.getClientReqPath(exchange));
    }

    private Mono<Result<ApiConfig>> auth(ServerWebExchange exchange, String app, String ip, String timestamp, String sign, String service, HttpMethod method, String path) {

        if (!systemConfig.isAggregateTestAuth()) {
            if (SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX0.equals(WebUtils.getClientReqPathPrefix(exchange))) {
                return Mono.just(Result.succ());
            }
        }

        Result<ApiConfig> r = get(app, service, method, path);
        if (r.code == Result.FAIL) {
            if (apiConfigServiceProperties.isNeedAuth()) {
                return Mono.just(r);
            } else {
                return Mono.just(Result.succ());
            }
        }

        ApiConfig ac = r.data;
        if (ac.checkApp) {
                App a = appService.getApp(app);
                if (a.useWhiteList && !a.allow(ip)) {
                        r.code = Result.FAIL;
                        r.msg  = ip + " not in " + app + " app white list";
                        return Mono.just(r);
                }
                if (a.useAuth) {
                        if (a.authType == App.AUTH_TYPE.SIGN) {
                            return authSign(a, timestamp, sign, r);
                        } else if (a.authType == App.AUTH_TYPE.SECRET_KEY) {
                            return authSecretKey(a, sign, r);
                        } else if (customAuth == null) {
                            r.code = Result.FAIL;
                            r.msg  = "no custom auth bean for " + app;
                            return Mono.just(r);
                        } else {
                            if (customAuth instanceof AbstractCustomAuth) {
                                AbstractCustomAuth abstractCustomAuth = (AbstractCustomAuth) customAuth;
                                return abstractCustomAuth.auth(app, ip, timestamp, sign, a, exchange)
                                                         .flatMap(
                                                             res -> {
                                                                 if (res.code == Result.FAIL) {
                                                                     r.code = res.code;
                                                                     r.msg  = res.msg;
                                                                 }
                                                                 return Mono.just(r);
                                                             }
                                                         );
                            } else {
                                return customAuth.auth(exchange, app, ip, timestamp, sign, a)
                                                 .flatMap(
                                                     v -> {
                                                         if (v == Access.YES) {
                                                             return Mono.just(r);
                                                         } else {
                                                             r.code = Result.FAIL;
                                                             r.msg = v.getReason();
                                                             return Mono.just(r);
                                                         }
                                                     }
                                                 );
                            }
                        }
                }
        }
        return Mono.just(r);
    }

    private Mono<Result<ApiConfig>> authSign(App a, String timestamp, String sign, Result<ApiConfig> r) {
        if (StringUtils.isAnyBlank(timestamp, sign)) {
            r.code = Result.FAIL;
            r.msg  = a.app + " not present timestamp " + timestamp + " or sign " + sign;
        } else if (validate(a.app, timestamp, a.secretkey, sign)) {
        } else {
            r.code = Result.FAIL;
            r.msg  = a.app + " sign " + sign + " invalid";
        }
        return Mono.just(r);
    }

    private boolean validate(String app, String timestamp, String secretKey, String sign) {
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(app)      .append(Consts.S.UNDER_LINE)
         .append(timestamp).append(Consts.S.UNDER_LINE)
         .append(secretKey);
        return sign.equalsIgnoreCase(DigestUtils.md532(b.toString()));
    }

    private Mono<Result<ApiConfig>> authSecretKey(App a, String sign, Result<ApiConfig> r) {
        if (StringUtils.isBlank(sign)) {
            r.code = Result.FAIL;
            r.msg  = a.app + " not present secret key " + sign;
        } else if (a.secretkey.equals(sign)) {
        } else {
            r.code = Result.FAIL;
            r.msg  = a.app + " secret key " + sign + " invalid";
        }
        return Mono.just(r);
    }

    private String getTimestamp(HttpHeaders reqHdrs) {
        List<String> tsHdrs = systemConfig.getTimestampHeaders();
        for (int i = 0; i < tsHdrs.size(); i++) {
            String v = reqHdrs.getFirst(tsHdrs.get(i));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String getSign(HttpHeaders reqHdrs) {
        List<String> signHdrs = systemConfig.getSignHeaders();
        for (int i = 0; i < signHdrs.size(); i++) {
            String v = reqHdrs.getFirst(signHdrs.get(i));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static class ApiConfigPathPatternComparator implements Comparator<ApiConfig> {

        private final String path;

        public ApiConfigPathPatternComparator(String path) {
            this.path = path;
        }

        @Override
        public int compare(ApiConfig ac1, ApiConfig ac2) {
            String pattern1 = ac1.path, pattern2 = ac2.path;
            ApiConfigPathPatternComparator.PatternInfo info1 = new ApiConfigPathPatternComparator.PatternInfo(pattern1);
            ApiConfigPathPatternComparator.PatternInfo info2 = new ApiConfigPathPatternComparator.PatternInfo(pattern2);

            if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
                return 0;
            }
            else if (info1.isLeastSpecific()) {
                return 1;
            }
            else if (info2.isLeastSpecific()) {
                return -1;
            }

            boolean pattern1EqualsPath = pattern1.equals(this.path);
            boolean pattern2EqualsPath = pattern2.equals(this.path);
            if (pattern1EqualsPath && pattern2EqualsPath) {
                return 0;
            }
            else if (pattern1EqualsPath) {
                return -1;
            }
            else if (pattern2EqualsPath) {
                return 1;
            }

            if (info1.isPrefixPattern() && info2.isPrefixPattern()) {
                return info2.getLength() - info1.getLength();
            }
            else if (info1.isPrefixPattern() && info2.getDoubleWildcards() == 0) {
                return 1;
            }
            else if (info2.isPrefixPattern() && info1.getDoubleWildcards() == 0) {
                return -1;
            }

            if (info1.getTotalCount() != info2.getTotalCount()) {
                return info1.getTotalCount() - info2.getTotalCount();
            }

            if (info1.getLength() != info2.getLength()) {
                return info2.getLength() - info1.getLength();
            }

            if (info1.getSingleWildcards() < info2.getSingleWildcards()) {
                return -1;
            }
            else if (info2.getSingleWildcards() < info1.getSingleWildcards()) {
                return 1;
            }

            if (info1.getUriVars() < info2.getUriVars()) {
                return -1;
            }
            else if (info2.getUriVars() < info1.getUriVars()) {
                return 1;
            }

            return 0;
        }

        private static class PatternInfo {

            private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?}");

            @Nullable
            private final String pattern;

            private int uriVars;

            private int singleWildcards;

            private int doubleWildcards;

            private boolean catchAllPattern;

            private boolean prefixPattern;

            @Nullable
            private Integer length;

            public PatternInfo(@Nullable String pattern) {
                this.pattern = pattern;
                if (this.pattern != null) {
                    initCounters();
                    this.catchAllPattern = this.pattern.equals("/**");
                    this.prefixPattern = !this.catchAllPattern && this.pattern.endsWith("/**");
                }
                if (this.uriVars == 0) {
                    this.length = (this.pattern != null ? this.pattern.length() : 0);
                }
            }

            protected void initCounters() {
                int pos = 0;
                if (this.pattern != null) {
                    while (pos < this.pattern.length()) {
                        if (this.pattern.charAt(pos) == '{') {
                            this.uriVars++;
                            pos++;
                        }
                        else if (this.pattern.charAt(pos) == '*') {
                            if (pos + 1 < this.pattern.length() && this.pattern.charAt(pos + 1) == '*') {
                                this.doubleWildcards++;
                                pos += 2;
                            }
                            else if (pos > 0 && !this.pattern.substring(pos - 1).equals(".*")) {
                                this.singleWildcards++;
                                pos++;
                            }
                            else {
                                pos++;
                            }
                        }
                        else {
                            pos++;
                        }
                    }
                }
            }

            public int getUriVars() {
                return this.uriVars;
            }

            public int getSingleWildcards() {
                return this.singleWildcards;
            }

            public int getDoubleWildcards() {
                return this.doubleWildcards;
            }

            public boolean isLeastSpecific() {
                return (this.pattern == null || this.catchAllPattern);
            }

            public boolean isPrefixPattern() {
                return this.prefixPattern;
            }

            public int getTotalCount() {
                return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
            }

            public int getLength() {
                if (this.length == null) {
                    this.length = (this.pattern != null ?
                            VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length() : 0);
                }
                return this.length;
            }
        }
    }
}
