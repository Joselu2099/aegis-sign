package com.aegis.sign.infrastructure.adapter.signature;

import io.micrometer.observation.ObservationRegistry;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SignatureServiceAdapterTest {

    private static Path tempKeystoreFile;
    private SignatureServiceAdapter signatureServiceAdapter;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @BeforeAll
    static void createTestKeystore() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=aegis-sign-test, O=Test, C=ES");
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 365L * 24 * 60 * 60 * 1000);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE, notBefore, notAfter, subject, kp.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(kp.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(signer));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("aegis-sign", kp.getPrivate(), "changeit".toCharArray(), new Certificate[]{cert});

        tempKeystoreFile = Files.createTempFile("test-keystore", ".p12");
        try (FileOutputStream fos = new FileOutputStream(tempKeystoreFile.toFile())) {
            ks.store(fos, "changeit".toCharArray());
        }
    }

    @AfterAll
    static void cleanup() {
        if (tempKeystoreFile != null) {
            try { Files.deleteIfExists(tempKeystoreFile); } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void setUp() {
        signatureServiceAdapter = new SignatureServiceAdapter(
                resourceLoader,
                ObservationRegistry.NOOP,
                "file:" + tempKeystoreFile.toAbsolutePath(),
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
                    assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(signature));
                })
                .verifyComplete();
    }

    @Test
    void signPdfShouldReturnSignedPdf() throws Exception {
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

                    try {
                        org.openpdf.text.pdf.PdfReader reader = new org.openpdf.text.pdf.PdfReader(signedPdf);
                        org.openpdf.text.pdf.AcroFields fields = reader.getAcroFields();
                        assertNotNull(fields, "AcroFields should not be null in signed document");
                    } catch (Exception e) {
                        fail("Failed to verify signed PDF: " + e.getMessage());
                    }
                })
                .verifyComplete();
    }
}
