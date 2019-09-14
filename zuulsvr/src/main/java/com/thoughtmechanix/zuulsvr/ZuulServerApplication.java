package com.thoughtmechanix.zuulsvr;

import java.util.Collections;
import java.util.List;

import com.thoughtmechanix.zuulsvr.filters.FilterUtils;
import com.thoughtmechanix.zuulsvr.utils.UserContextInterceptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableDiscoveryClient
public class ZuulServerApplication {

    @LoadBalanced
    @Bean
    public RestTemplate getRestTemplate(){
        RestTemplate template = new RestTemplate();
        List interceptors = template.getInterceptors();
        if (interceptors == null) {
            template.setInterceptors(Collections.singletonList(new UserContextInterceptor()));
        } else {
            interceptors.add(new UserContextInterceptor());
            template.setInterceptors(interceptors);
        }

        return template;
    }
    
    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }

    @Bean
    public GlobalFilter customGlobalPreFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getAttribute(FilterUtils.CORRELATION_ID);
            if (correlationId == null || correlationId.isEmpty()) {
                ServerHttpRequest request = 
                    exchange.getRequest().mutate().header(FilterUtils.CORRELATION_ID, generateCorrelationId()).build();
                exchange = exchange.mutate().request(request).build();
            }
            return chain.filter(exchange);
        };
    }

    @Bean
    public GlobalFilter customGlobalPostFilter() {
        return (exchange, chain) -> chain.filter(exchange).then(Mono.just(exchange)).map(serverWebExchange -> {
            String correlationId = serverWebExchange.getRequest().getHeaders().get(FilterUtils.CORRELATION_ID).get(0);
            serverWebExchange.getResponse().getHeaders().set(FilterUtils.CORRELATION_ID, correlationId);
            return serverWebExchange;
        }).then();
    }

    public static void main(String[] args) {
        SpringApplication.run(ZuulServerApplication.class, args);
    }
}
