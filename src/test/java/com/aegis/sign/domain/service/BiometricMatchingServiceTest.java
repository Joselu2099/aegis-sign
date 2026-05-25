package com.aegis.sign.domain.service;

import com.aegis.sign.domain.model.MatchResult;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class BiometricMatchingServiceTest {

    private final BiometricMatchingService biometricMatchingService = new BiometricMatchingService();

    @Test
    void shouldMatchSameFaces() {
        byte[] face1 = new byte[1000];
        byte[] face2 = new byte[1000]; // Same length, should have high similarity

        biometricMatchingService.match(face1, face2)
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertTrue(result.isMatch(), "Should match faces with same length");
                    assertTrue(result.getSimilarityScore() > 0.8, "Similarity should be high");
                    assertEquals(0.5, result.getLivenessScore(), "Default liveness should be 0.5 when no frames provided");
                })
                .verifyComplete();
    }

    @Test
    void shouldNotMatchDifferentFaces() {
        byte[] face1 = new byte[1000];
        byte[] face2 = new byte[500]; // Different length, ratio 0.5

        biometricMatchingService.match(face1, face2)
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertFalse(result.isMatch(), "Should not match faces with significant length difference");
                    assertTrue(result.getSimilarityScore() < 0.8, "Similarity should be low");
                })
                .verifyComplete();
    }

    @Test
    void shouldCheckLivenessWithFrames() {
        byte[] frames = new byte[1000];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = (byte) (i % 256); // Add variation
        }

        double liveness = biometricMatchingService.checkLiveness(frames);
        assertTrue(liveness > 0.8, "Liveness should be high with varying frames");
    }

    @Test
    void shouldFailLivenessWithStaticFrames() {
        byte[] frames = new byte[1000]; // All zeros, no variation

        double liveness = biometricMatchingService.checkLiveness(frames);
        assertTrue(liveness < 0.8, "Liveness should be low with static frames");
    }

    @Test
    void shouldIncludeLivenessInMatchResult() {
        byte[] face1 = new byte[1000];
        byte[] face2 = new byte[1000];
        byte[] frames = new byte[1000];
        for (int i = 0; i < frames.length; i++) frames[i] = (byte) i;

        biometricMatchingService.match(face1, face2, frames)
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertTrue(result.isMatch(), "Should be a match");
                    assertTrue(result.getLivenessScore() > 0.8, "Liveness should be high");
                    assertTrue(result.getConfidence() > 0.8, "Confidence should be high");
                })
                .verifyComplete();
    }

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
}
