package com.aegis.sign.domain.service;

import com.aegis.sign.domain.exception.TemplateNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TemplateResolverTest {

    private final TemplateResolver resolver = new TemplateResolver();

    @Test
    void resolve_validTemplate_returnsContent() {
        String content = resolver.resolve("valid-template");
        assertThat(content.trim()).isEqualTo("{\"key\": \"value\"}");
    }

    @Test
    void resolve_templateNotFound_throwsException() {
        assertThatThrownBy(() -> resolver.resolve("non-existent"))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessage("Template not found: non-existent");
    }

    @Test
    void resolve_ioException_throwsRuntimeException() {
        try (MockedConstruction<ClassPathResource> mocked = mockConstruction(ClassPathResource.class, (mock, context) -> {
            when(mock.exists()).thenReturn(true);
            when(mock.getInputStream()).thenThrow(new IOException("Simulated IO error"));
        })) {
            assertThatThrownBy(() -> resolver.resolve("some-template"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to read template: some-template")
                    .hasCauseInstanceOf(IOException.class);
        }
    }
}
