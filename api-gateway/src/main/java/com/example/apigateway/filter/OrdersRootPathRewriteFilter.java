package com.example.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Order-service uses {@code server.servlet.context-path=/orders}. Spring may redirect
 * {@code /orders} → {@code /orders/} with an absolute {@code Location} pointing at the
 * backend host:port (e.g. :8083). The browser then follows cross-origin and CORS blocks it.
 * <p>
 * Normalizing to {@code /orders/} before routing avoids that redirect leaking to the client.
 */
@Component
public class OrdersRootPathRewriteFilter implements GlobalFilter, Ordered {

    /** Run before route resolution (see {@link org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping}) */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String rawPath = request.getURI().getRawPath();
        if (!"/orders".equals(rawPath)) {
            return chain.filter(exchange);
        }
        ServerHttpRequest rewritten = request.mutate()
                .uri(UriComponentsBuilder.fromUri(request.getURI()).replacePath("/orders/").build().toUri())
                .build();
        return chain.filter(exchange.mutate().request(rewritten).build());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
