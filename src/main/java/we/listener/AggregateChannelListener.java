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

package we.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import we.fizz.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.List;

import static we.listener.AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_MESSAGE_LISTENER_CONTAINER;

/**
 * 聚合Channel监听器
 * @author zhongjie
 */
@Component
public class AggregateChannelListener {
    public AggregateChannelListener(@Qualifier(AGGREGATE_REACTIVE_REDIS_MESSAGE_LISTENER_CONTAINER)
                                    ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer,
                                    ConfigLoader configLoader) {
        this.reactiveRedisMessageListenerContainer = reactiveRedisMessageListenerContainer;
        this.configLoader = configLoader;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateChannelListener.class);
    /**
     * 聚合配置Channel名称，fizz-manager会往该Channel发送聚合变更信息
     */
    private static final String AGGREGATE_CHANNEL = "fizz_aggregate_channel";

    private ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer;
    private ConfigLoader configLoader;
    private Disposable disposable;

    @PostConstruct
    public void init() {
        Flux<ReactiveSubscription.Message<String, String>> aggregateMessageFlux =
                reactiveRedisMessageListenerContainer.receive(ChannelTopic.of(AGGREGATE_CHANNEL));
        disposable = aggregateMessageFlux.parallel().runOn(Schedulers.parallel()).subscribe(message -> {
            String messageBody = message.getMessage();
            LOGGER.info("获取到[{}]消息[{}]", AGGREGATE_CHANNEL, messageBody);
            try {
                this.handleAggregateMessage(messageBody);
            } catch (Exception e) {
                LOGGER.warn(String.format("处理聚合推送数据异常[%s]", message), e);
            }
        });
    }

    @PreDestroy
    public void destroy() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private static final String TYPE_PUBLISH = "publish ";
    private static final String TYPE_ROLLBACK = "rollback ";
    private static final String TYPE_TEST = "test ";
    private static final String TYPE_DELETE = "delete ";
    private static final String TYPE_REFRESH = "refresh ";
    private void handleAggregateMessage(String message) throws Exception {
        if (!StringUtils.hasText(message)) {
            return;
        }

        String type;
        String data;
        if (message.startsWith(TYPE_PUBLISH)) {
            type = TYPE_PUBLISH;
            data = message.replace(TYPE_PUBLISH, "");
        } else if (message.startsWith(TYPE_ROLLBACK)) {
            type = TYPE_ROLLBACK;
            data = message.replace(TYPE_ROLLBACK, "");
        } else if (message.startsWith(TYPE_TEST)) {
            type = TYPE_TEST;
            data = message.replace(TYPE_TEST, "");
        } else if (message.startsWith(TYPE_DELETE)) {
            type = TYPE_DELETE;
            data = message.replace(TYPE_DELETE, "");
        } else if (message.startsWith(TYPE_REFRESH)) {
            type = TYPE_REFRESH;
            // 需要刷新集合配置的节点IP列表
            data = message.replace(TYPE_REFRESH, "");
        } else {
            LOGGER.warn(String.format("未知的聚合推送数据[%s]", message));
            return;
        }

        switch (type) {
            case TYPE_PUBLISH:
            case TYPE_ROLLBACK:
            case TYPE_TEST:
                configLoader.addConfig(data);
                break;
            case TYPE_DELETE:
                configLoader.deleteConfig(data);
                break;
            case TYPE_REFRESH:
                this.refreshConfig(data);
                break;
            default:
                break;
        }
    }

    private static final TypeReference<List<String>> STRING_LIST_TYPE_REFERENCE = new TypeReference<List<String>>() {};
    private void refreshConfig(String allowIps) throws Exception {
        if (!this.checkIp(LOCAL_IP, allowIps)) {
            // 本机IP不在刷新列表中，直接返回
            LOGGER.info("本机IP地址[{}]不在刷新IP列表[{}]中", LOCAL_IP, allowIps);
            return;
        }

        // 刷新配置
        configLoader.init();
    }

    private boolean checkIp(String clientIp, String allowIps) {
        if (!StringUtils.hasText(allowIps)) {
            return true;
        }

        List<String> allowIpList = JSON.parseObject(allowIps, STRING_LIST_TYPE_REFERENCE);
        for (String allowIp : allowIpList) {
            boolean allow = "*".equals(allowIp) || allowIp.equals(clientIp)
                    || allowIp.contains("-") && ipIsValid(allowIp, clientIp);
            if (allow) {
                return true;
            }
        }
        return false;
    }

    private static final String REGX_IP = "((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)";
    private static final String REGX_IPB = REGX_IP + "\\-" + REGX_IP;

    private static boolean ipIsValid(String allowIp, String clientIp) {
        if (allowIp == null) {
            return false;
        }

        if (clientIp == null) {
            return false;
        }
        allowIp = allowIp.trim();
        clientIp = clientIp.trim();
        if (!allowIp.matches(REGX_IPB) || !clientIp.matches(REGX_IP)) {
            return false;
        }
        int idx = allowIp.indexOf('-');
        String[] sips = allowIp.substring(0, idx).split("\\.");
        String[] sipe = allowIp.substring(idx + 1).split("\\.");
        String[] sipt = clientIp.split("\\.");
        long ips = 0L, ipe = 0L, ipt = 0L;
        for (int i = 0; i < 4; ++i) {
            ips = ips << 8 | Integer.parseInt(sips[i]);
            ipe = ipe << 8 | Integer.parseInt(sipe[i]);
            ipt = ipt << 8 | Integer.parseInt(sipt[i]);
        }
        if (ips > ipe) {
            long t = ips;
            ips = ipe;
            ipe = t;
        }
        return ips <= ipt && ipt <= ipe;
    }

    private static final String LOCAL_IP = AggregateChannelListener.getLocalIp();
    private static String getLocalIp() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (Exception e) {
            LOGGER.warn("获取本地IP地址异常", e);
            return "unknown";
        }
    }
}
