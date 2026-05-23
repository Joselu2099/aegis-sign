package com.aegis.sign.domain.service;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OcrExtractorServiceTest {

    private final OcrExtractorService ocrExtractorService = new OcrExtractorService();

    @Test
    void shouldExtractData() {
        byte[] image = new byte[]{1, 2, 3};
        Map<String, String> results = ocrExtractorService.extractData(image);
        
        assertNotNull(results);
        assertTrue(results.containsKey("documentNumber"));
        assertEquals("L898902C3", results.get("documentNumber"));
    }

    @Test
    void shouldReturnEmptyMapForNullImage() {
        Map<String, String> results = ocrExtractorService.extractData(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
