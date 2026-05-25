package com.aegis.sign.domain.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.image.BufferedImage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OcrExtractorServiceTest {

    @Mock
    private ITesseract tesseract;

    private OcrExtractorService ocrExtractorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ocrExtractorService = new OcrExtractorService(tesseract);
    }

    @Test
    void shouldExtractDataUsingTesseract() throws TesseractException {
        // A minimal valid 1x1 GIF image
        byte[] imageBytes = new byte[]{
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, (byte) 0x80, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44,
            0x01, 0x00, 0x3b
        };
        
        String mockOcrResult = "ID: A1234567B DATE: 950101 EXP: 301231";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(mockOcrResult);

        Map<String, String> results = ocrExtractorService.extractData(imageBytes);
        
        assertNotNull(results);
        assertEquals(mockOcrResult, results.get("rawContent"));
        assertEquals("A1234567B", results.get("documentNumber"));
        assertEquals("950101", results.get("birthDate"));
        assertEquals("301231", results.get("expiryDate"));
    }

    @Test
    void shouldFallbackToMockDataOnTesseractException() throws TesseractException {
        byte[] imageBytes = new byte[]{0x47, 0x49, 0x46, 0x38}; // Invalid image header but enough to try
        when(tesseract.doOCR(any(BufferedImage.class))).thenThrow(new TesseractException("Failed"));

        Map<String, String> results = ocrExtractorService.extractData(imageBytes);
        
        assertNotNull(results);
        assertEquals("L898902C3", results.get("documentNumber")); // Check fallback
        assertNull(results.get("rawContent"));
    }

    @Test
    void shouldReturnEmptyMapForNullImage() {
        Map<String, String> results = ocrExtractorService.extractData(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFallbackOnInvalidImage() {
        byte[] invalidImage = new byte[]{1, 2, 3};
        Map<String, String> results = ocrExtractorService.extractData(invalidImage);
        
        assertNotNull(results);
        assertEquals("L898902C3", results.get("documentNumber"));
    }
}
