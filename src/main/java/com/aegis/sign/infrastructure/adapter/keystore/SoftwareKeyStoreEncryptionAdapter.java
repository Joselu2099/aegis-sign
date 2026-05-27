package com.aegis.sign.infrastructure.adapter.keystore;

import com.aegis.sign.domain.port.EncryptionPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class SoftwareKeyStoreEncryptionAdapter implements EncryptionPort {

    private SecretKey secretKey;
    private static final String ALGORITHM = "AES";

    public SoftwareKeyStoreEncryptionAdapter() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256); // 256-bit AES key
            this.secretKey = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize encryption key", e);
        }
    }

    @Override
    public Mono<String> encrypt(String data) {
        return Mono.fromCallable(() -> {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        });
    }

    @Override
    public Mono<String> decrypt(String encryptedData) {
        return Mono.fromCallable(() -> {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        });
    }
}
