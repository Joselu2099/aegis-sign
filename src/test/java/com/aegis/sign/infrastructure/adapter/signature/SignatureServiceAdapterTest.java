package com.aegis.sign.infrastructure.adapter.signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
// import com.lowagie.text.Document;
// import com.lowagie.text.Paragraph;
// import com.lowagie.text.pdf.PdfWriter;

import static org.junit.jupiter.api.Assertions.*;

class SignatureServiceAdapterTest {

    private SignatureServiceAdapter signatureServiceAdapter;
    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    @BeforeEach
    void setUp() {
        // Use the actual keystore from src/main/resources for testing
        signatureServiceAdapter = new SignatureServiceAdapter(
                resourceLoader,
                "classpath:keystore.p12",
                "changeit",
                "aegis-sign",
                "changeit"
        );
        signatureServiceAdapter.init();
    }

    @Test
    void signShouldReturnValidBase64String() {
        String contentHash = "abc123hash";
        String thumbprint = "thumbprint-789";

        StepVerifier.create(signatureServiceAdapter.sign(contentHash, thumbprint))
                .assertNext(signature -> {
                    assertNotNull(signature);
                    assertFalse(signature.isEmpty());
                    
                    // Verify it's valid Base64
                    assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(signature));
                })
                .verifyComplete();
    }

    @Test
    void signPdfShouldReturnSignedPdf() throws Exception {
        // Create a simple PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document();
        org.openpdf.text.pdf.PdfWriter.getInstance(document, out);
        document.open();
        document.add(new org.openpdf.text.Paragraph("Test PDF for signing"));
        document.close();
        byte[] unsignedPdf = out.toByteArray();

        StepVerifier.create(signatureServiceAdapter.signPdf(unsignedPdf))
                .assertNext(signedPdf -> {
                    assertNotNull(signedPdf);
                    assertTrue(signedPdf.length > unsignedPdf.length);
                })
                .verifyComplete();
    }
}
