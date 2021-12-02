package we.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import we.Fizz;

import javax.annotation.Resource;
import java.time.Duration;

//@Configuration
public class FizzWebClientConfig {

    /*public static final String FIZZ_LOAD_BALANCED_WEB_CLIENT = "fizzLoadBalancedWebClient";

    @Bean(FIZZ_LOAD_BALANCED_WEB_CLIENT)
    public WebClient LoadBalancedWebClient(@Qualifier(ProxyWebClientConfig.proxyWebClient) WebClient sourceWebClient,
                                           ReactorLoadBalancerExchangeFilterFunction reactorLoadBalancerExchangeFilterFunction) {

        return sourceWebClient.mutate().filter(reactorLoadBalancerExchangeFilterFunction).build();
    }*/

    // disable the Resilience4J auto-configuration spring.cloud.circuitbreaker.resilience4j.enabled = false

    /*@Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(
                              id -> new Resilience4JConfigBuilder(id)
                                  .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                                  .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(4)).build())
                                  .build()
                          );
    }*/

    /*@Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> slowCustomizer() {
        return factory -> {
                   factory.configure(
                           builder -> builder
                                   .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                                   .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build()),
                           "slow", "slowflux"
                   );
                   factory.addCircuitBreakerCustomizer(
                           Customizer.once(
                                   circuitBreaker -> circuitBreaker.getEventPublisher()
                                           .onError(null)    // normalFluxErrorConsumer
                                           .onSuccess(null), // normalFluxSuccessConsumer
                                   circuitBreaker -> circuitBreaker.getName()
                           ),
                           "normalflux"
                   );
               };
    }*/

    /*@Resource
    private ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory;

    public void x() {
        WebClient webClient = (WebClient) Fizz.context.getBean(ProxyWebClientConfig.proxyWebClient);
        webClient.get().uri("/slow").retrieve().bodyToMono(String.class)
                 .transform(
                         stringMono -> reactiveResilience4JCircuitBreakerFactory.create("slow")
                                       .run(stringMono, throwable -> Mono.just("fallback"))
                 );
    }*/
}
