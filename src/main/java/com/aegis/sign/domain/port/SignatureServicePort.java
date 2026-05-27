package com.aegis.sign.domain.port;

import reactor.core.publisher.Mono;

public interface SignatureServicePort {
    Mono<String> sign(String contentHash, String certificateThumbprint);
    Mono<byte[]> signPdf(byte[] pdfContent);
}
