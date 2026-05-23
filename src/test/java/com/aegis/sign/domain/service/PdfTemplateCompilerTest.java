package com.aegis.sign.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfTemplateCompilerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PdfTemplateCompiler compiler = new PdfTemplateCompiler(objectMapper);

    @Test
    void testCompilePdf() {
        String template = """
                {
                  "elements": [
                    {"type": "header", "text": "Contract for {{name}}"},
                    {"type": "paragraph", "text": "This agreement is valid until {{date}}."}
                  ]
                }
                """;
        
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John Doe");
        data.put("date", "2026-12-31");

        byte[] result = compiler.compile(template, data);

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Basic check for PDF magic number %PDF-
        assertEquals('%', (char) result[0]);
        assertEquals('P', (char) result[1]);
        assertEquals('D', (char) result[2]);
        assertEquals('F', (char) result[3]);
    }
}
