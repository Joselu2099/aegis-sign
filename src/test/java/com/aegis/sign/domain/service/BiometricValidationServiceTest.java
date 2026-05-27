package com.aegis.sign.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BiometricValidationServiceTest {

    private BiometricValidationService service;

    @BeforeEach
    void setUp() {
        service = new BiometricValidationService();
    }

    @Test
    void testValidate_ValidImage() throws IOException {
        byte[] imageBytes = createMockImage(500, 500, Color.GRAY);
        BiometricValidationService.ValidationResult result = service.validate(imageBytes);

        assertTrue(result.isValid(), "Image should be valid: " + result.getErrorMessage());
        assertEquals(500, result.getWidth());
        assertEquals(500, result.getHeight());
        assertTrue(result.isFaceDetected());
        assertTrue(result.getLivenessScore() >= 0.5);
    }

    @Test
    void testValidate_LowResolution() throws IOException {
        byte[] imageBytes = createMockImage(400, 400, Color.GRAY);
        BiometricValidationService.ValidationResult result = service.validate(imageBytes);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Resolution too low"));
    }

    @Test
    void testValidate_LowContrast() throws IOException {
        // Create a solid color image which has 0 contrast
        byte[] imageBytes = createMockImage(500, 500, Color.BLACK);
        BiometricValidationService.ValidationResult result = service.validate(imageBytes);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("contrast too low"));
    }

    @Test
    void testValidate_InvalidFormat() {
        byte[] imageBytes = "not an image".getBytes();
        BiometricValidationService.ValidationResult result = service.validate(imageBytes);

        assertFalse(result.isValid());
        assertEquals("Invalid image format", result.getErrorMessage());
    }

    private byte[] createMockImage(int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        
        // Add some noise to avoid 0 contrast if needed
        if (color.equals(Color.GRAY)) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(10, 10, 100, 100);
            g2d.setColor(Color.BLACK);
            g2d.fillRect(200, 200, 100, 100);
        }
        
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        
        // Ensure size > 5KB for mock face detection
        byte[] bytes = baos.toByteArray();
        if (bytes.length < 5120) {
            byte[] padded = new byte[6000];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            return padded;
        }
        return bytes;
    }
}
