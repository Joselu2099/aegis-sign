package com.aegis.sign.domain.service;

import com.aegis.sign.domain.exception.TemplateNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class TemplateResolver {

    private static final String TEMPLATE_PATH_FORMAT = "templates/%s.json";

    public String resolve(String templateId) {
        ClassPathResource resource = new ClassPathResource(String.format(TEMPLATE_PATH_FORMAT, templateId));
        if (!resource.exists()) {
            throw new TemplateNotFoundException("Template not found: " + templateId);
        }
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read template: {}", templateId, e);
            throw new RuntimeException("Failed to read template: " + templateId, e);
        }
    }
}
