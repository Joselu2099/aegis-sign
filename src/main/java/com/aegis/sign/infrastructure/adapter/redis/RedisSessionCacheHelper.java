package com.aegis.sign.infrastructure.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisSessionCacheHelper {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Mono<Boolean> put(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    public <T> Mono<T> get(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, clazz));
                    } catch (JsonProcessingException e) {
                        return Mono.error(e);
                    }
                });
    }

    public Mono<Boolean> delete(String key) {
        return redisTemplate.opsForValue().delete(key);
    }
}
