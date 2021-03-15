package we.proxy.grpc;

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

/**
 * gRPC instance service implementation, get all config from redis cache when init and listen on redis channel for change
 *
 * @author zhongjie
 */
@Service
public class GrpcInstanceServiceImpl implements GrpcInstanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcInstanceServiceImpl.class);
    /**
     *  redis rpc service change channel
     */
    private static final String RPC_SERVICE_CHANNEL = "fizz_rpc_service_channel";
    /**
     * redis rpc service info hash key
     */
    private static final String RPC_SERVICE_HASH_KEY = "fizz_rpc_service";

    private static Map<String, List<String>> serviceToInstancesMap = new ConcurrentHashMap<>(32);
    private static Map<Long, RpcService> idToRpcServiceMap = new ConcurrentHashMap<>(32);
    private static Map<String, AtomicLong> serviceToCountMap = new ConcurrentHashMap<>(32);

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() throws Throwable {
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
                        this.updateLocalCache(rpcService);
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
                    return lsnRpcServiceChange();
                }
        ).block();
        if (error != ReactorUtils.EMPTY_THROWABLE) {
            assert error != null;
            throw error;
        }
    }

    @Override
    public String getInstanceRandom(String service) {
        List<String> instanceList = serviceToInstancesMap.get(service);
        if (CollectionUtils.isEmpty(instanceList)) {
            return null;
        }
        return instanceList.get(ThreadLocalRandom.current().nextInt(instanceList.size()));
    }

    @Override
    public String getInstanceRoundRobin(String service) {
        List<String> instanceList = serviceToInstancesMap.get(service);
        if (CollectionUtils.isEmpty(instanceList)) {
            return null;
        }
        long currentCount = serviceToCountMap.computeIfAbsent(service, it -> new AtomicLong()).getAndIncrement();
        return instanceList.get((int)currentCount % instanceList.size());
    }

    @Override
    public List<String> getAllInstance(String service) {
        return serviceToInstancesMap.get(service);
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
                this.updateLocalCache(rpcService);
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

    private void updateLocalCache(RpcService rpcService) {
        if (rpcService.getIsDeleted() == RpcService.DELETED) {
            RpcService removedRpcService = idToRpcServiceMap.remove(rpcService.id);
            LOGGER.info("remove {}", removedRpcService);
            if (removedRpcService != null) {
                serviceToInstancesMap.remove(removedRpcService.getService());
                serviceToCountMap.remove(removedRpcService.getService());
            }
        } else {
            RpcService existRpcService = idToRpcServiceMap.get(rpcService.id);
            idToRpcServiceMap.put(rpcService.id, rpcService);
            if (existRpcService == null) {
                LOGGER.info("add {}", rpcService);
            } else {
                LOGGER.info("update {} with {}", existRpcService, rpcService);
                serviceToInstancesMap.remove(existRpcService.getService());
                serviceToCountMap.remove(existRpcService.getService());
            }
            serviceToInstancesMap.put(rpcService.service, rpcService.instance == null ? Collections.emptyList() :
                    Arrays.asList(rpcService.getInstance().split(",")));
        }
    }

    private static class RpcService {
        private static final int DELETED = 1;
        private Long id;
        private Integer isDeleted;
        private String service;
        private String instance;

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
    }
}
