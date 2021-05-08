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
package we.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.AggregateRedisConfig;
import we.flume.clients.log4j2appender.LogService;
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.ReactorUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * RPC instance service implementation, get all config from redis cache when init and listen on redis channel for change
 *
 * @author zhongjie
 */
@Service
public class RpcInstanceServiceImpl implements RpcInstanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcInstanceServiceImpl.class);
    /**
     *  redis rpc service change channel
     */
    private static final String RPC_SERVICE_CHANNEL = "fizz_rpc_service_channel";
    /**
     * redis rpc service info hash key
     */
    private static final String RPC_SERVICE_HASH_KEY = "fizz_rpc_service";

    /**
     * key pattern of {@link #serviceToInstancesMap}, {@link #serviceToLoadBalanceTypeMap} and {@link #serviceToCountMap}
     * {rpc type}-{service name}
     */
    private static final String SERVICE_KEY_PATTERN = "%s-%s";

    private static final Byte LOAD_BALANCE_TYPE_ROUND_ROBIN = 1;
    private static final Byte LOAD_BALANCE_TYPE_RANDOM = 2;

    private static Map<String, List<String>> serviceToInstancesMap = new ConcurrentHashMap<>(32);
    private static Map<String, Byte> serviceToLoadBalanceTypeMap = new ConcurrentHashMap<>(32);
    private static Map<Long, RpcService> idToRpcServiceMap = new ConcurrentHashMap<>(32);
    private static Map<String, AtomicLong> serviceToCountMap = new ConcurrentHashMap<>(32);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() throws Throwable {
        this.init(this::lsnRpcServiceChange);
    }

    @Override
    public void refreshLocalCache() throws Throwable {
        this.init(null);
    }

    private void init(Supplier<Mono<Throwable>> doAfterLoadCache) throws Throwable {
        Map<String, List<String>> serviceToInstancesMapTmp = new ConcurrentHashMap<>(32);
        Map<String, Byte> serviceToLoadBalanceTypeMapTmp = new ConcurrentHashMap<>(32);
        Map<Long, RpcService> idToRpcServiceMapTmp = new ConcurrentHashMap<>(32);
        Map<String, AtomicLong> serviceToCountMapTmp = new ConcurrentHashMap<>(32);

        final Throwable[] throwable = new Throwable[1];
        Throwable error = Mono.just(Objects.requireNonNull(redisTemplate.opsForHash().entries(RPC_SERVICE_HASH_KEY)
                .defaultIfEmpty(new AbstractMap.SimpleEntry<>(ReactorUtils.OBJ, ReactorUtils.OBJ)).onErrorStop().doOnError(t -> LOGGER.info(null, t))
                .concatMap(e -> {
                    Object k = e.getKey();
                    if (k == ReactorUtils.OBJ) {
                        return Flux.just(e);
                    }
                    Object v = e.getValue();
                    LOGGER.info(k.toString() + Constants.Symbol.COLON + v.toString(), LogService.BIZ_ID, k.toString());
                    String json = (String) v;
                    try {
                        RpcService rpcService = JacksonUtils.readValue(json, RpcService.class);
                        this.updateLocalCache(rpcService, serviceToInstancesMapTmp, serviceToLoadBalanceTypeMapTmp,
                                idToRpcServiceMapTmp, serviceToCountMapTmp);
                        return Flux.just(e);
                    } catch (Throwable t) {
                        throwable[0] = t;
                        LOGGER.info(json, t);
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
            assert error != null;
            throw error;
        }

        serviceToInstancesMap = serviceToInstancesMapTmp;
        serviceToLoadBalanceTypeMap = serviceToLoadBalanceTypeMapTmp;
        idToRpcServiceMap = idToRpcServiceMapTmp;
        serviceToCountMap = serviceToCountMapTmp;
    }

    @Override
    public String getInstance(RpcTypeEnum rpcTypeEnum, String service) {
        Byte loadBalanceType = serviceToLoadBalanceTypeMap.get(this.getServiceKey(rpcTypeEnum.getType(), service));
        if (LOAD_BALANCE_TYPE_RANDOM.equals(loadBalanceType)) {
            LOGGER.debug("type:{} service:{} get instance random", rpcTypeEnum, service);
            return this.getInstanceRandom(rpcTypeEnum, service);
        } else {
            LOGGER.debug("type:{} service:{} get instance round-robin", rpcTypeEnum, service);
            return this.getInstanceRoundRobin(rpcTypeEnum, service);
        }
    }

    private String getInstanceRandom(RpcTypeEnum rpcTypeEnum, String service) {
        List<String> instanceList = this.getAllInstance(rpcTypeEnum, service);
        if (CollectionUtils.isEmpty(instanceList)) {
            return null;
        }
        if (instanceList.size() == 1) {
            return instanceList.get(0);
        }

        return instanceList.get(ThreadLocalRandom.current().nextInt(instanceList.size()));
    }

    private String getInstanceRoundRobin(RpcTypeEnum rpcTypeEnum, String service) {
        List<String> instanceList = this.getAllInstance(rpcTypeEnum, service);
        if (CollectionUtils.isEmpty(instanceList)) {
            return null;
        }
        if (instanceList.size() == 1) {
            return instanceList.get(0);
        }

        long currentCount = serviceToCountMap.computeIfAbsent(this.getServiceKey(rpcTypeEnum.getType(), service),
                it -> new AtomicLong()).getAndIncrement();
        return instanceList.get((int)currentCount % instanceList.size());
    }

    private List<String> getAllInstance(RpcTypeEnum rpcTypeEnum, String service) {
        return serviceToInstancesMap.get(this.getServiceKey(rpcTypeEnum.getType(), service));
    }

    private Mono<Throwable> lsnRpcServiceChange() {
        final Throwable[] throwable = new Throwable[1];
        final boolean[] b = {false};
        redisTemplate.listenToChannel(RPC_SERVICE_CHANNEL).doOnError(t -> {
            throwable[0] = t;
            b[0] = false;
            LOGGER.error("lsn " + RPC_SERVICE_CHANNEL, t);
        }).doOnSubscribe(
                s -> {
                    b[0] = true;
                    LOGGER.info("success to lsn on " + RPC_SERVICE_CHANNEL);
                }
        ).doOnNext(msg -> {
            String json = msg.getMessage();
            LOGGER.info(json, LogService.BIZ_ID, "rpc" + System.currentTimeMillis());
            try {
                RpcService rpcService = JacksonUtils.readValue(json, RpcService.class);
                this.updateLocalCache(rpcService, serviceToInstancesMap, serviceToLoadBalanceTypeMap, idToRpcServiceMap,
                        serviceToCountMap);
            } catch (Throwable t) {
                LOGGER.info(json, t);
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

    private void updateLocalCache(RpcService rpcService, Map<String, List<String>> serviceToInstancesMap,
                                  Map<String, Byte> serviceToLoadBalanceTypeMap, Map<Long, RpcService> idToRpcServiceMap,
                                  Map<String, AtomicLong> serviceToCountMap) {
        if (rpcService.getType() == null) {
            // historical gRPC data type and loadBalanceType is null, here set default value
            rpcService.setType(RpcTypeEnum.gRPC.getType());
            rpcService.setLoadBalanceType(LOAD_BALANCE_TYPE_ROUND_ROBIN);
        }

        if (rpcService.getIsDeleted() == RpcService.DELETED) {
            RpcService removedRpcService = idToRpcServiceMap.remove(rpcService.getId());
            LOGGER.info("remove {}", removedRpcService);
            if (removedRpcService != null) {
                serviceToInstancesMap.remove(this.getServiceKey(rpcService));
                serviceToLoadBalanceTypeMap.remove(this.getServiceKey(rpcService));
                serviceToCountMap.remove(this.getServiceKey(rpcService));
            }
        } else {
            RpcService existRpcService = idToRpcServiceMap.get(rpcService.getId());
            idToRpcServiceMap.put(rpcService.getId(), rpcService);
            if (existRpcService == null) {
                LOGGER.info("add {}", rpcService);
            } else {
                LOGGER.info("update {} with {}", existRpcService, rpcService);
                serviceToInstancesMap.remove(this.getServiceKey(existRpcService));
                serviceToLoadBalanceTypeMap.remove(this.getServiceKey(existRpcService));
                serviceToCountMap.remove(this.getServiceKey(existRpcService));
            }
            serviceToInstancesMap.put(this.getServiceKey(rpcService), rpcService.getInstance() == null ? Collections.emptyList() :
                    Arrays.asList(rpcService.getInstance().split(",")));
            serviceToLoadBalanceTypeMap.put(this.getServiceKey(rpcService), rpcService.getLoadBalanceType());
        }
    }

    private String getServiceKey(RpcService rpcService) {
        return String.format(SERVICE_KEY_PATTERN, rpcService.getType(), rpcService.getService());
    }

    private String getServiceKey(Byte type, String service) {
        return String.format(SERVICE_KEY_PATTERN, type, service);
    }

    static class RpcService {
        private static final int DELETED = 1;
        private Long id;
        private Integer isDeleted;
        private String service;
        private String instance;
        /**
         * RPC type: 2-gRPC 3-HTTP
         */
        private Byte type;
        /**
         * load balance type: 1-round-robin 2-random
         */
        private Byte loadBalanceType;

        @Override
        public String toString() {
            return JacksonUtils.writeValueAsString(this);
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getIsDeleted() {
            return isDeleted;
        }

        public void setIsDeleted(Integer isDeleted) {
            this.isDeleted = isDeleted;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getInstance() {
            return instance;
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }

        public Byte getType() {
            return type;
        }

        public void setType(Byte type) {
            this.type = type;
        }

        public Byte getLoadBalanceType() {
            return loadBalanceType;
        }

        public void setLoadBalanceType(Byte loadBalanceType) {
            this.loadBalanceType = loadBalanceType;
        }
    }
}
