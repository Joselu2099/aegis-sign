package com.aegis.sign.infrastructure.adapter.signature;

import com.aegis.sign.domain.port.SignatureServicePort;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.*;
import java.util.Base64;

@Service
public class SignatureServiceAdapter implements SignatureServicePort {

    private final PrivateKey privateKey;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public SignatureServiceAdapter() throws NoSuchAlgorithmException {
        // For demonstration purposes, generate a temporary RSA key pair.
        // In a real scenario, this would be loaded from a secure KeyStore.
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
    }

    @Override
    public Mono<String> sign(String contentHash, String certificateThumbprint) {
        return Mono.fromCallable(() -> {
            Signature signature = Signature.getInstance("SHA256withRSA", "BC");
            signature.initSign(privateKey);
            signature.update(contentHash.getBytes());
            byte[] signedData = signature.sign();
            return Base64.getEncoder().encodeToString(signedData);
        }).onErrorResume(e -> Mono.error(new RuntimeException("Error during digital signing", e)));
    }
}
