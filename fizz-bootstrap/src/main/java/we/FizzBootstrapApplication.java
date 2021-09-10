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

package we;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.couchbase.CouchbaseReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.ldap.LdapRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.solr.SolrRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastJpaDependencyAutoConfiguration;
import org.springframework.boot.autoconfigure.influx.InfluxDbAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketRequesterAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketServerAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.rsocket.RSocketSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import we.config.AggregateRedisConfig;
import we.log.LogSendAppender;

/**
 * fizz gateway application boot entrance
 *
 * @author linwaiwai
 * @author Francis Dong
 * @author hongqiaowei
 * @author zhongjie
 */
@SpringBootApplication(
    exclude = {
            ErrorWebFluxAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisReactiveAutoConfiguration.class,

            EmbeddedLdapAutoConfiguration.class,
            LdapAutoConfiguration.class,
            LdapRepositoriesAutoConfiguration.class,
            JndiConnectionFactoryAutoConfiguration.class,
            JndiDataSourceAutoConfiguration.class,

            HypermediaAutoConfiguration.class,
            MustacheAutoConfiguration.class,
            ThymeleafAutoConfiguration.class,
            FreeMarkerAutoConfiguration.class,

            RSocketMessagingAutoConfiguration.class,
            RSocketRequesterAutoConfiguration.class,
            RSocketSecurityAutoConfiguration.class,
            RSocketServerAutoConfiguration.class,
            RSocketStrategiesAutoConfiguration.class,

            SpringDataWebAutoConfiguration.class,

            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            XADataSourceAutoConfiguration.class,
            H2ConsoleAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            JtaAutoConfiguration.class,
            TransactionAutoConfiguration.class,

            FlywayAutoConfiguration.class,
            InfluxDbAutoConfiguration.class,
            LiquibaseAutoConfiguration.class,

            JpaRepositoriesAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JooqAutoConfiguration.class,

            MongoAutoConfiguration.class,
            EmbeddedMongoAutoConfiguration.class,
            MongoReactiveAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            MongoRepositoriesAutoConfiguration.class,
            MongoReactiveDataAutoConfiguration.class,

            CouchbaseAutoConfiguration.class,
            CouchbaseReactiveDataAutoConfiguration.class,

            CassandraAutoConfiguration.class,
            CassandraReactiveDataAutoConfiguration.class,

            SolrAutoConfiguration.class,
            SolrRepositoriesAutoConfiguration.class,
            ElasticsearchDataAutoConfiguration.class,
            ElasticsearchRepositoriesAutoConfiguration.class,

            JmsAutoConfiguration.class,
            ActiveMQAutoConfiguration.class,
            KafkaAutoConfiguration.class,
            ArtemisAutoConfiguration.class,
            RabbitAutoConfiguration.class,

            MailSenderAutoConfiguration.class,
            MailSenderValidatorAutoConfiguration.class,

            Neo4jDataAutoConfiguration.class,

            HazelcastAutoConfiguration.class,
            HazelcastJpaDependencyAutoConfiguration.class,

            CacheAutoConfiguration.class,
            BatchAutoConfiguration.class,
            IntegrationAutoConfiguration.class,

            JmxAutoConfiguration.class,
            SpringApplicationAdminJmxAutoConfiguration.class,

            OAuth2ClientAutoConfiguration.class,
            ReactiveOAuth2ClientAutoConfiguration.class,
            ReactiveOAuth2ResourceServerAutoConfiguration.class,
            QuartzAutoConfiguration.class
    }
)
@EnableDiscoveryClient
public class FizzBootstrapApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(FizzBootstrapApplication.class);

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(FizzBootstrapApplication.class);
        springApplication.setApplicationContextClass(CustomReactiveWebServerApplicationContext.class);
        FizzAppContext.appContext = springApplication.run(args);
    }

    private static class CustomReactiveWebServerApplicationContext extends AnnotationConfigReactiveWebServerApplicationContext {
        @Override
        protected void onClose() {
            super.onClose();
            if (AggregateRedisConfig.proxyLettuceConnectionFactory != null) {
                LOGGER.info("FizzBootstrapApplication stopped.");
                // set LogSendAppender.logEnabled to false to stop send log to fizz-manager
                LogSendAppender.logEnabled = Boolean.FALSE;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
                // the ProxyLettuceConnectionFactory remove DisposableBean interface, so invoke destroy method here
                AggregateRedisConfig.proxyLettuceConnectionFactory.destroy();
            }
        }
    }
}
