package com.aegis.sign.domain.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper for OCR (Optical Character Recognition) extraction logic using Tess4j.
 */
@Service
public class OcrExtractorService {

    private static final Logger log = LoggerFactory.getLogger(OcrExtractorService.class);

    // TD3 (Passport) - 2 lines of 44 characters
    private static final Pattern TD3_LINE_1_PATTERN = Pattern.compile("^P[A-Z0-9<]{1}[A-Z]{3}[A-Z0-9<]{39}$", Pattern.MULTILINE);
    private static final Pattern TD3_LINE_2_PATTERN = Pattern.compile("^[A-Z0-9<]{9}[0-9]{1}[A-Z]{3}[0-9]{6}[0-9]{1}[M|F|<]{1}[0-9]{6}[0-9]{1}[A-Z0-9<]{14}[0-9]{1}[0-9]{1}$", Pattern.MULTILINE);

    // TD1 (ID Card) - 3 lines of 30 characters
    private static final Pattern TD1_LINE_1_PATTERN = Pattern.compile("^[I|A|C][A-Z0-9<]{1}[A-Z]{3}[A-Z0-9<]{9}[0-9]{1}[A-Z0-9<]{15}$", Pattern.MULTILINE);
    private static final Pattern TD1_LINE_2_PATTERN = Pattern.compile("^[0-9]{6}[0-9]{1}[M|F|<]{1}[0-9]{6}[0-9]{1}[A-Z]{3}[A-Z0-9<]{11}[0-9]{1}$", Pattern.MULTILINE);
    private static final Pattern TD1_LINE_3_PATTERN = Pattern.compile("^[A-Z0-9<]{30}$", Pattern.MULTILINE);

    // TD2 (ID Card) - 2 lines of 36 characters
    private static final Pattern TD2_LINE_1_PATTERN = Pattern.compile("^[I|A|C][A-Z0-9<]{1}[A-Z]{3}[A-Z0-9<]{31}$", Pattern.MULTILINE);
    private static final Pattern TD2_LINE_2_PATTERN = Pattern.compile("^[A-Z0-9<]{9}[0-9]{1}[A-Z]{3}[0-9]{6}[0-9]{1}[M|F|<]{1}[0-9]{6}[0-9]{1}[A-Z0-9<]{7}[0-9]{1}$", Pattern.MULTILINE);

    // Basic fields that might be present outside MRZ or as general fallback if no MRZ found
    private static final Pattern DOC_NUM_PATTERN = Pattern.compile("[A-Z][0-9]{7}[A-Z0-9]");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{6}\\b");

    private final ITesseract tesseract;
    private final ObservationRegistry observationRegistry;

    @Value("${tesseract.datapath}")
    private String tessdataPath;

    public OcrExtractorService(ITesseract tesseract, ObservationRegistry observationRegistry) {
        this.tesseract = tesseract;
        this.observationRegistry = observationRegistry;
    }

    @PostConstruct
    public void init() {
        if (tessdataPath != null && !tessdataPath.isEmpty()) {
            tesseract.setDatapath(tessdataPath);
            log.info("Tesseract data path set to: {}", tessdataPath);
        } else {
            log.warn("Tesseract data path not configured. Using default Tesseract data path.");
        }
    }

    /**
     * Extracts data from an image.
     * 
     * @param imageBytes The image data as a byte array.
     * @return A map containing extracted field names and their values.
     */
    public Map<String, String> extractData(byte[] imageBytes) {
        return Observation.createNotStarted("ocr.extraction", observationRegistry)
                .observe(() -> doExtractData(imageBytes));
    }

    private Map<String, String> doExtractData(byte[] imageBytes) {
        Map<String, String> extractedFields = new HashMap<>();
        
        if (imageBytes == null || imageBytes.length == 0) {
            return extractedFields;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                log.warn("Could not decode image from byte array.");
                throw new com.aegis.sign.domain.exception.KycUserException("Could not decode image.", "INVALID_IMAGE_FORMAT");
            }

            String result = tesseract.doOCR(bufferedImage);
            extractedFields.put("rawContent", result);
            
            parseFields(result, extractedFields);

        } catch (TesseractException | IOException e) {
            log.error("OCR extraction failed: {}", e.getMessage());
            throw new com.aegis.sign.domain.exception.KycTechnicalException("OCR engine down.", "OCR_ENGINE_DOWN", e);
        } catch (Error e) {
            // Catch UnsatisfiedLinkError if native Tesseract libraries are missing
            log.error("Tesseract native libraries not available: {}", e.getMessage());
            throw new com.aegis.sign.domain.exception.KycTechnicalException("OCR engine down.", "OCR_ENGINE_DOWN", e);
        }

        return extractedFields;
    }

    private void parseFields(String text, Map<String, String> fields) {
        if (text == null || text.isBlank()) {
            return;
        }

        String[] lines = text.split("\\R"); // Split by any newline character

        // Attempt to find TD3
        if (lines.length >= 2 && TD3_LINE_1_PATTERN.matcher(lines[0]).matches() && TD3_LINE_2_PATTERN.matcher(lines[1]).matches()) {
            fields.put("mrzType", "TD3");
            fields.put("mrzLine1", lines[0]);
            fields.put("mrzLine2", lines[1]);
            extractFieldsFromTD3(lines[1], fields);
        }
        // Attempt to find TD1
        else if (lines.length >= 3 && TD1_LINE_1_PATTERN.matcher(lines[0]).matches() && TD1_LINE_2_PATTERN.matcher(lines[1]).matches() && TD1_LINE_3_PATTERN.matcher(lines[2]).matches()) {
            fields.put("mrzType", "TD1");
            fields.put("mrzLine1", lines[0]);
            fields.put("mrzLine2", lines[1]);
            fields.put("mrzLine3", lines[2]);
            extractFieldsFromTD1(lines[0], lines[1], fields);
        }
        // Attempt to find TD2
        else if (lines.length >= 2 && TD2_LINE_1_PATTERN.matcher(lines[0]).matches() && TD2_LINE_2_PATTERN.matcher(lines[1]).matches()) {
            fields.put("mrzType", "TD2");
            fields.put("mrzLine1", lines[0]);
            fields.put("mrzLine2", lines[1]);
            extractFieldsFromTD2(lines[1], fields);
        }

        // Basic fields that might be present outside MRZ or as general fallback if no MRZ found
        // Example regex for a Document Number (e.g., L898902C3)
        Matcher docNumMatcher = DOC_NUM_PATTERN.matcher(text);
        if (docNumMatcher.find() && !fields.containsKey("documentNumber")) { // Only if not already extracted from MRZ
            fields.put("documentNumber", docNumMatcher.group());
        }

        // Example regex for dates in YYMMDD format
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        int dateCount = 0;
        while (dateMatcher.find()) {
            if (dateCount == 0 && !fields.containsKey("birthDate")) fields.put("birthDate", dateMatcher.group());
            else if (dateCount == 1 && !fields.containsKey("expiryDate")) fields.put("expiryDate", dateMatcher.group());
            dateCount++;
        }
    }

    private void extractFieldsFromTD3(String mrzLine2, Map<String, String> fields) {
        // TD3 Line 2: Document number (9 chars), check_doc (1), nationality (3), DOB (6), check_dob (1),
        // Sex (1), Expiry (6), check_expiry (1), personal number (14), check_personal (1), composite check (1)
        fields.put("documentNumber", mrzLine2.substring(0, 9));
        fields.put("documentNumberCheckDigit", mrzLine2.substring(9, 10));
        fields.put("nationality", mrzLine2.substring(10, 13));
        fields.put("birthDate", mrzLine2.substring(13, 19));
        fields.put("birthDateCheckDigit", mrzLine2.substring(19, 20));
        fields.put("sex", mrzLine2.substring(20, 21));
        fields.put("expiryDate", mrzLine2.substring(21, 27));
        fields.put("expiryDateCheckDigit", mrzLine2.substring(27, 28));
        fields.put("personalNumber", mrzLine2.substring(28, 42));
        fields.put("personalNumberCheckDigit", mrzLine2.substring(42, 43));
        fields.put("compositeCheckDigit", mrzLine2.substring(43, 44));
    }

    private void extractFieldsFromTD1(String mrzLine1, String mrzLine2, Map<String, String> fields) {
        // TD1 Line 1: Document type (1), subclass (1), issuing state (3), document number (9), check_doc (1), optional (15)
        fields.put("documentNumber", mrzLine1.substring(5, 14));
        fields.put("documentNumberCheckDigit", mrzLine1.substring(14, 15));

        // TD1 Line 2: DOB (6), check_dob (1), sex (1), expiry (6), check_expiry (1), nationality (3), optional (11), composite check (1)
        fields.put("birthDate", mrzLine2.substring(0, 6));
        fields.put("birthDateCheckDigit", mrzLine2.substring(6, 7));
        fields.put("sex", mrzLine2.substring(7, 8));
        fields.put("expiryDate", mrzLine2.substring(8, 14));
        fields.put("expiryDateCheckDigit", mrzLine2.substring(14, 15));
        fields.put("nationality", mrzLine2.substring(15, 18));
        fields.put("compositeCheckDigit", mrzLine2.substring(29, 30));
    }

    private void extractFieldsFromTD2(String mrzLine2, Map<String, String> fields) {
        // TD2 Line 2: Document number (9 chars), check_doc (1), nationality (3), DOB (6), check_dob (1),
        // Sex (1), Expiry (6), check_expiry (1), optional (7), composite check (1)
        fields.put("documentNumber", mrzLine2.substring(0, 9));
        fields.put("documentNumberCheckDigit", mrzLine2.substring(9, 10));
        fields.put("nationality", mrzLine2.substring(10, 13));
        fields.put("birthDate", mrzLine2.substring(13, 19));
        fields.put("birthDateCheckDigit", mrzLine2.substring(19, 20));
        fields.put("sex", mrzLine2.substring(20, 21));
        fields.put("expiryDate", mrzLine2.substring(21, 27));
        fields.put("expiryDateCheckDigit", mrzLine2.substring(27, 28));
        fields.put("compositeCheckDigit", mrzLine2.substring(35, 36));
    }
}
