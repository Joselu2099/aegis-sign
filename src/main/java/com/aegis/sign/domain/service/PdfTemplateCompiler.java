package com.aegis.sign.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfTemplateCompiler {

    private final ObjectMapper objectMapper;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    /**
     * Compiles a PDF from a JSON template and dynamic data.
     *
     * @param jsonTemplate The JSON string defining the document structure.
     * @param data         Dynamic data to substitute in the template.
     * @return PDF content as byte array.
     */
    public byte[] compile(String jsonTemplate, Map<String, Object> data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            JsonNode root = objectMapper.readTree(jsonTemplate);
            
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            JsonNode elements = root.get("elements");
            if (elements != null && elements.isArray()) {
                for (JsonNode element : elements) {
                    processElement(document, element, data);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error compiling PDF template", e);
            throw new RuntimeException("Failed to compile PDF template", e);
        }
    }

    private void processElement(Document document, JsonNode element, Map<String, Object> data) throws Exception {
        String type = element.get("type").asText();
        String text = element.has("text") ? element.get("text").asText() : "";
        text = substituteVariables(text, data);

        switch (type.toLowerCase()) {
            case "header":
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
                document.add(new Paragraph(text, headerFont));
                break;
            case "paragraph":
                document.add(new Paragraph(text));
                break;
            default:
                log.warn("Unknown element type: {}", type);
        }
    }

    private String substituteVariables(String text, Map<String, Object> data) {
        if (data == null || text == null) return text;
        
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = data.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
