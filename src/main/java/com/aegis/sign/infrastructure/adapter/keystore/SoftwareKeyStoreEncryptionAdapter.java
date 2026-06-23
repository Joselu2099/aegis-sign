package com.aegis.sign.infrastructure.adapter.keystore;

import com.aegis.sign.domain.port.EncryptionPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SoftwareKeyStoreEncryptionAdapter implements EncryptionPort {

    private SecretKey secretKey;
    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public SoftwareKeyStoreEncryptionAdapter() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGen.init(256); // 256-bit AES key
            this.secretKey = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize encryption key", e);
        }
    }

    @Override
    public Mono<String> encrypt(String data) {
        return Mono.fromCallable(() -> {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> decrypt(String encryptedData) {
        return Mono.fromCallable(() -> {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, decodedBytes, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(decodedBytes, GCM_IV_LENGTH, decodedBytes.length - GCM_IV_LENGTH);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
