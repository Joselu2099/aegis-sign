package com.aegis.sign.domain.service;

import com.aegis.sign.domain.model.MatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BiometricMatchingService.class)
class BiometricMatchingServiceRealTest {

    @Autowired
    private BiometricMatchingService biometricMatchingService;

    @Test
    void shouldHandleNullInputsGracefully() {
        biometricMatchingService.match(null, null)
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertFalse(result.isMatch());
                    assertEquals(0.0, result.getSimilarityScore());
                })
                .verifyComplete();
    }

    @Test
    void shouldFallbackToMockWhenModelNotFound() {
        byte[] face1 = new byte[1000];
        byte[] face2 = new byte[1000];

        biometricMatchingService.match(face1, face2)
                .as(StepVerifier::create)
                .assertNext(result -> {
                    // Since model is not found in test env, it should use mock logic
                    assertTrue(result.getSimilarityScore() > 0.0);
                })
                .verifyComplete();
    }
}
