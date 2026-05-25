package com.aegis.sign.domain.service;

import com.aegis.sign.domain.model.MatchResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Random;

/**
 * Service for biometric matching and verification.
 */
@Service
public class BiometricMatchingService {

    private static final double DEFAULT_THRESHOLD = 0.8;
    private final Random random = new Random();

    /**
     * Performs a biometric match between a document face and a selfie face.
     *
     * @param documentFace The face image from the document.
     * @param selfieFace   The face image from the live selfie.
     * @return A Mono containing the MatchResult.
     */
    public Mono<MatchResult> match(byte[] documentFace, byte[] selfieFace) {
        return match(documentFace, selfieFace, null);
    }

    /**
     * Performs a biometric match between a document face and a selfie face,
     * including liveness detection using selfie frames.
     *
     * @param documentFace The face image from the document.
     * @param selfieFace   The face image from the live selfie.
     * @param selfieFrames Frames from the selfie video for liveness detection.
     * @return A Mono containing the MatchResult.
     */
    public Mono<MatchResult> match(byte[] documentFace, byte[] selfieFace, byte[] selfieFrames) {
        return Mono.fromCallable(() -> {
            if (documentFace == null || selfieFace == null) {
                return MatchResult.builder()
                        .isMatch(false)
                        .similarityScore(0.0)
                        .livenessScore(0.0)
                        .confidence(0.0)
                        .build();
            }

            double similarityScore = calculateMockSimilarity(documentFace, selfieFace);
            double livenessScore = checkLiveness(selfieFrames);
            
            // If frames were null, we use a neutral liveness score.
            if (selfieFrames == null) {
                livenessScore = 0.5;
            }

            double confidence = (similarityScore + livenessScore) / 2.0;
            boolean isMatch = similarityScore >= DEFAULT_THRESHOLD;

            return MatchResult.builder()
                    .isMatch(isMatch)
                    .similarityScore(similarityScore)
                    .livenessScore(livenessScore)
                    .confidence(confidence)
                    .build();
        });
    }

    private double calculateMockSimilarity(byte[] face1, byte[] face2) {
        if (face1.length == 0 || face2.length == 0) return 0.0;
        
        // Mock logic: similarity based on length ratio + small random factor
        double ratio = Math.min(face1.length, face2.length) / (double) Math.max(face1.length, face2.length);
        double variance = (random.nextDouble() * 0.1) - 0.05; // +/- 0.05
        return Math.min(1.0, Math.max(0.0, ratio + variance));
    }

    /**
     * Simulates liveness detection by checking frame variation.
     *
     * @param selfieFrames Byte array representing frames.
     * @return Liveness score between 0.0 and 1.0.
     */
    public double checkLiveness(byte[] selfieFrames) {
        if (selfieFrames == null || selfieFrames.length == 0) {
            return 0.0;
        }

        // Simulate variation check: at least 500 bytes and some byte variation
        boolean hasVariation = false;
        for (int i = 1; i < Math.min(selfieFrames.length, 100); i++) {
            if (selfieFrames[i] != selfieFrames[0]) {
                hasVariation = true;
                break;
            }
        }

        if (selfieFrames.length > 500 && hasVariation) {
            return 0.9 + (random.nextDouble() * 0.1);
        } else {
            return 0.3 + (random.nextDouble() * 0.3);
        }
    }
}
