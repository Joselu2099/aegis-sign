package com.aegis.sign.infrastructure.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSessionCacheHelperTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    private RedisSessionCacheHelper redisSessionCacheHelper;

    @BeforeEach
    void setUp() {
        redisSessionCacheHelper = new RedisSessionCacheHelper(redisTemplate, objectMapper);
    }

    @Test
    void put_Success() throws JsonProcessingException {
        // Arrange
        String key = "testKey";
        String value = "testValue";
        Duration ttl = Duration.ofMinutes(5);
        String jsonValue = "\"testValue\"";

        when(objectMapper.writeValueAsString(value)).thenReturn(jsonValue);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(key, jsonValue, ttl)).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(redisSessionCacheHelper.put(key, value, ttl))
                .expectNext(true)
                .verifyComplete();

        verify(objectMapper).writeValueAsString(value);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(key, jsonValue, ttl);
    }

    @Test
    void put_JsonProcessingException() throws JsonProcessingException {
        // Arrange
        String key = "testKey";
        String value = "testValue";
        Duration ttl = Duration.ofMinutes(5);

        JsonProcessingException exception = new JsonProcessingException("Test Exception") {};
        when(objectMapper.writeValueAsString(value)).thenThrow(exception);

        // Act & Assert
        StepVerifier.create(redisSessionCacheHelper.put(key, value, ttl))
                .expectErrorMatches(e -> e instanceof JsonProcessingException && e.getMessage().equals("Test Exception"))
                .verify();

        verify(objectMapper).writeValueAsString(value);
    }

    @Test
    void get_Success() throws JsonProcessingException {
        // Arrange
        String key = "testKey";
        String jsonValue = "\"testValue\"";
        String expectedValue = "testValue";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(Mono.just(jsonValue));
        when(objectMapper.readValue(jsonValue, String.class)).thenReturn(expectedValue);

        // Act & Assert
        StepVerifier.create(redisSessionCacheHelper.get(key, String.class))
                .expectNext(expectedValue)
                .verifyComplete();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(key);
        verify(objectMapper).readValue(jsonValue, String.class);
    }

    @Test
    void get_CacheMiss() {
        // Arrange
        String key = "testKey";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(redisSessionCacheHelper.get(key, String.class))
                .verifyComplete();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(key);
    }

    @Test
    void get_JsonProcessingException() throws JsonProcessingException {
        // Arrange
        String key = "testKey";
        String jsonValue = "invalidJson";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(Mono.just(jsonValue));

        JsonProcessingException exception = new JsonProcessingException("Test Exception") {};
        when(objectMapper.readValue(jsonValue, String.class)).thenThrow(exception);

        // Act & Assert
        StepVerifier.create(redisSessionCacheHelper.get(key, String.class))
                .expectErrorMatches(e -> e instanceof JsonProcessingException && e.getMessage().equals("Test Exception"))
                .verify();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(key);
        verify(objectMapper).readValue(jsonValue, String.class);
    }

    @Test
    void delete_Success() {
        // Arrange
        String key = "testKey";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.delete(key)).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(redisSessionCacheHelper.delete(key))
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).delete(key);
    }
}
