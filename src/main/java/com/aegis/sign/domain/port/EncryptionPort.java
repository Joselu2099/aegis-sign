package com.aegis.sign.domain.port;

import reactor.core.publisher.Mono;

public interface EncryptionPort {
    Mono<String> encrypt(String data);
    Mono<String> decrypt(String encryptedData);
    Mono<byte[]> signPdf(byte[] pdfContent);
}
