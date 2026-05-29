package com.aegis.sign.domain.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class BiometricValidationService {

    @Data
    @Builder
    public static class ValidationResult {
        private boolean isValid;
        private String errorMessage;
        private String errorCode;
        private double contrast;
        private int width;
        private int height;
        private boolean faceDetected;
        private double livenessScore;
    }

    public ValidationResult validate(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return ValidationResult.builder()
                        .isValid(false)
                        .errorMessage("Invalid image format")
                        .errorCode("INVALID_IMAGE_FORMAT")
                        .build();
            }

            int width = image.getWidth();
            int height = image.getHeight();
            double contrast = calculateContrast(image);

            // Basic quality checks
            if (width < 480 || height < 480) {
                return ValidationResult.builder()
                        .isValid(false)
                        .errorMessage("Resolution too low. Minimum 480x480 required.")
                        .errorCode("LOW_RESOLUTION")
                        .width(width)
                        .height(height)
                        .contrast(contrast)
                        .build();
            }

            if (contrast < 10.0) { // Lowered threshold for mock purposes
                return ValidationResult.builder()
                        .isValid(false)
                        .errorMessage("Image contrast too low.")
                        .errorCode("BLURRY_DOCUMENT")
                        .width(width)
                        .height(height)
                        .contrast(contrast)
                        .build();
            }

            // Mock face detection (in a real scenario, use OpenCV or AI model)
            boolean faceDetected = detectFaceMock(imageBytes);
            if (!faceDetected) {
                return ValidationResult.builder()
                        .isValid(false)
                        .errorMessage("No face detected in the image.")
                        .errorCode("FACE_NOT_DETECTED")
                        .width(width)
                        .height(height)
                        .contrast(contrast)
                        .faceDetected(false)
                        .build();
            }

            // Mock liveness detection
            double livenessScore = calculateLivenessMock(imageBytes);
            if (livenessScore < 0.6) {
                 return ValidationResult.builder()
                        .isValid(false)
                        .errorMessage("Liveness check failed.")
                        .errorCode("LIVENESS_FAILED")
                        .width(width)
                        .height(height)
                        .contrast(contrast)
                        .faceDetected(true)
                        .livenessScore(livenessScore)
                        .build();
            }

            return ValidationResult.builder()
                    .isValid(true)
                    .width(width)
                    .height(height)
                    .contrast(contrast)
                    .faceDetected(true)
                    .livenessScore(livenessScore)
                    .build();

        } catch (IOException e) {
            return ValidationResult.builder()
                    .isValid(false)
                    .errorMessage("Error processing image: " + e.getMessage())
                    .errorCode("IMAGE_PROCESSING_ERROR")
                    .build();
        }
    }

    private double calculateContrast(BufferedImage image) {
        // Simple contrast calculation based on pixel intensity variance
        long sum = 0;
        long sumSq = 0;
        int count = 0;
        for (int x = 0; x < image.getWidth(); x += 10) {
            for (int y = 0; y < image.getHeight(); y += 10) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);
                int intensity = (r + g + b) / 3;
                sum += intensity;
                sumSq += (long) intensity * intensity;
                count++;
            }
        }
        if (count == 0) return 0.0;
        double mean = (double) sum / count;
        double variance = ((double) sumSq / count) - (mean * mean);
        return Math.sqrt(Math.max(0, variance));
    }

    private boolean detectFaceMock(byte[] imageBytes) {
        // Heuristic: if image size is > 5KB, assume it might have a face for mock
        return imageBytes.length > 5120;
    }

    private double calculateLivenessMock(byte[] imageBytes) {
        // Mock liveness: deterministic value based on hash for testing
        int hash = java.util.Arrays.hashCode(imageBytes);
        return 0.5 + (Math.abs(hash % 40) / 100.0); 
    }
}
