package com.aegis.sign.infrastructure.web.filter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TokenBucketRateLimiterFilter implements WebFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    // Configuration - could be moved to application.yml
    private final long capacity = 100;
    private final long refillRatePerSecond = 10;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String ip = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";

        Bucket bucket = buckets.computeIfAbsent(ip, k -> new Bucket(capacity, refillRatePerSecond));

        if (bucket.tryConsume()) {
            return chain.filter(exchange);
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
    }

    private static class Bucket {
        private final long capacity;
        private final long refillRate;
        private final AtomicLong tokens;
        private final AtomicLong lastRefillTimestamp;

        public Bucket(long capacity, long refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTimestamp = new AtomicLong(Instant.now().getEpochSecond());
        }

        public boolean tryConsume() {
            refill();
            long currentTokens = tokens.get();
            while (currentTokens > 0) {
                if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                    return true;
                }
                currentTokens = tokens.get();
            }
            return false;
        }

        private void refill() {
            long now = Instant.now().getEpochSecond();
            long lastRefill = lastRefillTimestamp.get();
            
            if (now > lastRefill) {
                long elapsedTime = now - lastRefill;
                long tokensToAdd = elapsedTime * refillRate;
                
                if (tokensToAdd > 0) {
                    if (lastRefillTimestamp.compareAndSet(lastRefill, now)) {
                        tokens.updateAndGet(current -> Math.min(capacity, current + tokensToAdd));
                    }
                }
            }
        }
    }
}
