package com.aegis.sign.domain.service;

import com.aegis.sign.domain.exception.TemplateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateResolverTest {

    private TemplateResolver templateResolver;

    @BeforeEach
    void setUp() {
        templateResolver = new TemplateResolver();
    }

    @Test
    void shouldResolveExistingTemplate() {
        String result = templateResolver.resolve("sample-contract");
        assertNotNull(result);
        assertTrue(result.contains("Contrato de Alquiler"));
    }

    @Test
    void shouldThrowExceptionForMissingTemplate() {
        assertThrows(TemplateNotFoundException.class, () -> templateResolver.resolve("non-existing-template"));
    }
}
