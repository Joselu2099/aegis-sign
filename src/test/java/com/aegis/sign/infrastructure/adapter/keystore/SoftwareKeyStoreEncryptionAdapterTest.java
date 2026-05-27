package com.aegis.sign.infrastructure.adapter.keystore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SoftwareKeyStoreEncryptionAdapterTest {

    private SoftwareKeyStoreEncryptionAdapter encryptionAdapter;

    @BeforeEach
    void setUp() {
        encryptionAdapter = new SoftwareKeyStoreEncryptionAdapter();
    }

    @Test
    void encryptAndDecrypt_shouldReturnOriginalData() {
        String originalData = "This is a test string to be encrypted and decrypted.";

        Mono<String> encryptedMono = encryptionAdapter.encrypt(originalData);

        StepVerifier.create(encryptedMono)
                .assertNext(encryptedData -> {
                    assertNotNull(encryptedData);

                    Mono<String> decryptedMono = encryptionAdapter.decrypt(encryptedData);

                    StepVerifier.create(decryptedMono)
                            .assertNext(decryptedData -> {
                                assertNotNull(decryptedData);
                                assertEquals(originalData, decryptedData);
                            })
                            .expectComplete()
                            .verify();
                })
                .expectComplete()
                .verify();
    }
}
