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

package we.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import we.fizz.ConfigLoader;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.ApiConifg2appsService;
import we.plugin.auth.AppService;
import we.plugin.auth.GatewayGroupService;
import we.proxy.RpcInstanceService;
import we.stats.ratelimit.ResourceRateLimitConfigService;

import javax.annotation.Resource;

/**
 * refresh config local cache config
 * @see ApiConfigService#refreshLocalCache() refresh api config local cache
 * @see ApiConifg2appsService#refreshLocalCache() refresh api config to apps local cache
 * @see ConfigLoader#refreshLocalCache()  refresh aggregate config local cache
 * @see GatewayGroupService#refreshLocalCache() refresh gateway group local cache
 * @see AppService#refreshLocalCache() refresh app local cache
 * @see ResourceRateLimitConfigService#refreshLocalCache() refresh flow control rule local cache
 * @see RpcInstanceService#refreshLocalCache() refresh rpc service local cache
 *
 * @author zhongjie
 */
@Configuration
public class RefreshLocalCacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshLocalCacheConfig.class);

    @Resource
    private RefreshLocalCacheConfigProperties refreshLocalCacheConfigProperties;

    @Resource
    private ConfigLoader configLoader;

    @Resource
    private ApiConfigService apiConfigService;

    @Resource
    private ApiConifg2appsService apiConifg2appsService;

    @Resource
    private GatewayGroupService gatewayGroupService;

    @Resource
    private AppService appService;

    @Resource
    private ResourceRateLimitConfigService resourceRateLimitConfigService;

    @Resource
    private RpcInstanceService rpcInstanceService;

    @Scheduled(initialDelayString = "${refresh-local-cache.initial-delay-millis:300000}",
            fixedRateString = "${refresh-local-cache.fixed-rate-millis:300000}")
    public void refreshLocalCache() {
        if (refreshLocalCacheConfigProperties.isApiConfigCacheRefreshEnabled()) {
            LOGGER.debug("refresh api config local cache");
            try {
                apiConfigService.refreshLocalCache();
            } catch (Throwable t) {
                LOGGER.warn("refresh api config local cache exception", t);
            }
        }

        if (refreshLocalCacheConfigProperties.isApiConfig2AppsCacheRefreshEnabled()) {
            LOGGER.debug("refresh api config to apps local cache");
            try {
                apiConifg2appsService.refreshLocalCache();
            } catch (Throwable t) {
                LOGGER.warn("refresh api config to apps local cache exception", t);
            }
        }

        if (refreshLocalCacheConfigProperties.isAggregateConfigCacheRefreshEnabled()) {
            LOGGER.debug("refresh aggregate config local cache");
            try {
                configLoader.refreshLocalCache();
            } catch (Exception e) {
                LOGGER.warn("refresh aggregate config local cache exception", e);
            }
        }

        if (refreshLocalCacheConfigProperties.isGatewayGroupCacheRefreshEnabled()) {
            LOGGER.debug("refresh gateway group local cache");
            try {
                gatewayGroupService.refreshLocalCache();
            } catch (Throwable t) {
                LOGGER.warn("refresh gateway group local cache exception", t);
            }
        }

        if (refreshLocalCacheConfigProperties.isAppAuthCacheRefreshEnabled()) {
            LOGGER.debug("refresh app auth local cache");
            try {
                appService.refreshLocalCache();
            } catch (Throwable t) {
                LOGGER.warn("refresh app auth local cache exception", t);
            }
        }

        if (refreshLocalCacheConfigProperties.isFlowControlRuleCacheRefreshEnabled()) {
            LOGGER.debug("refresh flow control rule local cache");
            try {
                resourceRateLimitConfigService.refreshLocalCache();
            } catch (Throwable t) {
                LOGGER.warn("refresh flow control rule local cache exception", t);
            }
        }

        if (refreshLocalCacheConfigProperties.isRpcServiceCacheRefreshEnabled()) {
            LOGGER.debug("refresh rpc service local cache");
            try {
                rpcInstanceService.refreshLocalCache();
            } catch (Throwable t) {
                LOGGER.warn("refresh rpc service local cache exception", t);
            }
        }
    }
}
