package com.aegis.sign.domain.service;

import org.springframework.stereotype.Service;

/**
 * Service for biometric matching and verification.
 */
@Service
public class BiometricMatchingService {

    private static final double DEFAULT_THRESHOLD = 0.8;

    /**
     * Calculates a matching score between two biometric templates.
     * 1:1 face match score calculation.
     * 
     * @param sourceTemplate Biometric data from source (e.g., ID document).
     * @param targetTemplate Biometric data from target (e.g., live selfie).
     * @return A match score between 0.0 and 1.0.
     */
    public double calculateMatchScore(byte[] sourceTemplate, byte[] targetTemplate) {
        if (sourceTemplate == null || targetTemplate == null) {
            return 0.0;
        }

        // Mock implementation of matching score calculation.
        // In a real scenario, this would involve comparing feature vectors
        // extracted from facial images using a deep learning model.
        
        // Simulating a calculation based on length as a placeholder
        if (sourceTemplate.length == targetTemplate.length) {
            return 0.95;
        }
        
        return 0.75;
    }

    /**
     * Verifies if two biometric templates match based on a threshold.
     * 
     * @param sourceTemplate Biometric data from source.
     * @param targetTemplate Biometric data from target.
     * @param threshold Matching threshold.
     * @return true if score >= threshold.
     */
    public boolean verifyMatch(byte[] sourceTemplate, byte[] targetTemplate, double threshold) {
        return calculateMatchScore(sourceTemplate, targetTemplate) >= threshold;
    }

    /**
     * Verifies if two biometric templates match using the default threshold.
     * 
     * @param sourceTemplate Biometric data from source.
     * @param targetTemplate Biometric data from target.
     * @return true if match is verified.
     */
    public boolean verifyMatch(byte[] sourceTemplate, byte[] targetTemplate) {
        return verifyMatch(sourceTemplate, targetTemplate, DEFAULT_THRESHOLD);
    }
}
