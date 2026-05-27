package com.aegis.sign.domain.port;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StoragePort {
    Mono<String> upload(byte[] content, String path);
    Mono<byte[]> download(String path);
    Flux<String> listOlderThan(int days);
    Mono<Void> delete(String path);

    // New methods for temporary storage
    Mono<String> uploadTempFile(byte[] content, String path);
    Flux<String> listTempFilesOlderThan(int days);
    Mono<Void> deleteTempFile(String path);
}

