package com.aegis.sign.infrastructure.adapter.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class TokenBucketRateLimiterFilter implements WebFilter {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> script;
    
    @Value("${rate-limit.kyc.capacity:10}")
    private long capacity;
    
    @Value("${rate-limit.kyc.refill-rate:1}")
    private long refillRate;

    public TokenBucketRateLimiterFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("scripts/rate_limit.lua"));
        redisScript.setResultType(Long.class);
        this.script = redisScript;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("Filtering request for path: {}", path);

        if (!path.startsWith("/api/v1/kyc")) {
            log.info("Path does not start with /api/v1/kyc, skipping rate limit for path: {}", path);
            return chain.filter(exchange);
        }

        String ip = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";

        String key = "rate_limit:kyc:" + ip;
        long now = Instant.now().getEpochSecond();

        log.info("Applying rate limit for IP: {}, Key: {}, Capacity: {}, Refill Rate: {}, Now: {}", ip, key, capacity, refillRate, now);

        return redisTemplate.execute(script, List.of(key), List.of(String.valueOf(capacity), String.valueOf(refillRate), String.valueOf(now)))
                .next()
                .flatMap(allowed -> {
                    log.info("Redis script returned allowed: {} for IP: {} on path: {}", allowed, ip, path);
                    if (allowed == 1L) {
                        return chain.filter(exchange);
                    } else {
                        log.warn("Rate limit exceeded for IP: {} on path: {}", ip, path);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                });
    }
}
