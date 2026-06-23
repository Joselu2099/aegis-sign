package com.aegis.sign.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = BiometricMatchingService.class,
    properties = {
        "spring.cloud.vault.enabled=false",
        "spring.config.import=",
        "keystore.path=classpath:test-keystore.p12",
        "keystore.password=changeit",
        "keystore.alias=aegis-sign",
        "keystore.key-password=changeit",
        "biometrics.model-path=src/main/resources/models/face_embedding.onnx",
        "biometrics.match-threshold=0.8",
        "biometrics.input-size=112"
    }
)
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
