package com.aegis.sign.infrastructure.adapter.signature;

import com.aegis.sign.domain.port.SignatureServicePort;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfSignatureAppearance;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Base64;

@Slf4j
@Service
public class SignatureServiceAdapter implements SignatureServicePort {

    private final ResourceLoader resourceLoader;
    private final ObservationRegistry observationRegistry;
    private final String keystorePath;
    private final String keystorePassword;
    private final String keyAlias;
    private final String keyPassword;

    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public SignatureServiceAdapter(
            ResourceLoader resourceLoader,
            ObservationRegistry observationRegistry,
            @Value("${keystore.path}") String keystorePath,
            @Value("${keystore.password}") String keystorePassword,
            @Value("${keystore.alias}") String keyAlias,
            @Value("${keystore.key-password}") String keyPassword) {
        this.resourceLoader = resourceLoader;
        this.observationRegistry = observationRegistry;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
    }

    @PostConstruct
    public void init() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            Resource resource = resourceLoader.getResource(keystorePath);
            keyStore.load(resource.getInputStream(), keystorePassword.toCharArray());

            this.privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
            this.certificateChain = keyStore.getCertificateChain(keyAlias);

            if (privateKey == null) {
                throw new RuntimeException("Private key not found for alias: " + keyAlias);
            }
            log.info("KeyStore loaded successfully with alias: {}", keyAlias);
        } catch (Exception e) {
            log.error("Error loading KeyStore", e);
            throw new RuntimeException("Failed to initialize SignatureServiceAdapter", e);
        }
    }

    @Override
    public Mono<String> sign(String contentHash, String certificateThumbprint) {
        return Mono.defer(() -> {
            Observation observation = Observation.start("signature.operation", observationRegistry);
            return Mono.fromCallable(() -> {
                try (Observation.Scope scope = observation.openScope()) {
                    Signature signature = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
                    signature.initSign(privateKey);
                    signature.update(contentHash.getBytes());
                    byte[] signedData = signature.sign();
                    return Base64.getEncoder().encodeToString(signedData);
                }
            })
            .doOnSuccess(s -> observation.stop())
            .doOnError(e -> {
                observation.error(e);
                observation.stop();
            })
            .onErrorResume(e -> {
                log.error("Error during digital signing", e);
                return Mono.error(new RuntimeException("Error during digital signing", e));
            });
        });
    }

    @Override
    public Mono<byte[]> signPdf(byte[] pdfContent) {
        return Mono.defer(() -> {
            Observation observation = Observation.start("signature.pdf.operation", observationRegistry);
            return Mono.fromCallable(() -> {
                try (Observation.Scope scope = observation.openScope()) {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        PdfReader reader = new PdfReader(pdfContent);
                        // The 'true' parameter allows multiple signatures
                        PdfStamper stamper = PdfStamper.createSignature(reader, baos, null, null, true);
                        
                        PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
                        // Set signature details
                        appearance.setReason("Electronic Signature");
                        appearance.setLocation("Aegis Sign Platform");
                        appearance.setCrypto(privateKey, certificateChain, null, PdfSignatureAppearance.SELF_SIGNED);
                        
                        stamper.close();
                        return baos.toByteArray();
                    }
                }
            })
            .doOnSuccess(s -> observation.stop())
            .doOnError(e -> {
                observation.error(e);
                observation.stop();
            })
            .onErrorResume(e -> {
                log.error("Error signing PDF", e);
                return Mono.error(new RuntimeException("Error signing PDF", e));
            });
        });
    }
}
