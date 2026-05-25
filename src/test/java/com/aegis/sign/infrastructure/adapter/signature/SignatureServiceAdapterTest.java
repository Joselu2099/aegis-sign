package com.aegis.sign.infrastructure.adapter.signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SignatureServiceAdapterTest {

    private SignatureServiceAdapter signatureServiceAdapter;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        signatureServiceAdapter = new SignatureServiceAdapter();
    }

    @Test
    void signShouldReturnValidBase64String() {
        String contentHash = "abc123hash";
        String thumbprint = "thumbprint-789";

        StepVerifier.create(signatureServiceAdapter.sign(contentHash, thumbprint))
                .assertNext(signature -> {
                    assertNotNull(signature);
                    assertFalse(signature.isEmpty());
                    assertFalse(signature.startsWith("signed-"), "Should not return dummy signature");
                    
                    // Verify it's valid Base64
                    try {
                        Base64.getDecoder().decode(signature);
                    } catch (IllegalArgumentException e) {
                        fail("Signature is not a valid Base64 string");
                    }
                })
                .verifyComplete();
    }

    @Test
    void signShouldReturnDifferentSignaturesForDifferentHashes() {
        String hash1 = "hash1";
        String hash2 = "hash2";
        String thumbprint = "thumbprint";

        String sig1 = signatureServiceAdapter.sign(hash1, thumbprint).block();
        String sig2 = signatureServiceAdapter.sign(hash2, thumbprint).block();

        assertNotNull(sig1);
        assertNotNull(sig2);
        assertNotEquals(sig1, sig2);
    }
}
