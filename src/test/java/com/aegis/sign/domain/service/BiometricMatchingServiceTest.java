package com.aegis.sign.domain.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BiometricMatchingServiceTest {

    private final BiometricMatchingService biometricMatchingService = new BiometricMatchingService();

    @Test
    void shouldCalculateMatchScore() {
        byte[] source = new byte[]{1, 2, 3};
        byte[] target = new byte[]{1, 2, 3};
        
        double score = biometricMatchingService.calculateMatchScore(source, target);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertEquals(0.95, score);
    }

    @Test
    void shouldVerifyMatch() {
        byte[] source = new byte[]{1, 2, 3};
        byte[] target = new byte[]{1, 2, 3};
        
        assertTrue(biometricMatchingService.verifyMatch(source, target));
    }

    @Test
    void shouldNotVerifyMatchWhenScoreIsLow() {
        byte[] source = new byte[]{1, 2, 3};
        byte[] target = new byte[]{1, 2, 3, 4}; // different length in mock implementation returns 0.75
        
        assertFalse(biometricMatchingService.verifyMatch(source, target, 0.8));
    }
}
