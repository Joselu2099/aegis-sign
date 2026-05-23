package com.aegis.sign.domain.port;

import reactor.core.publisher.Mono;

public interface StoragePort {
    Mono<String> upload(byte[] content, String path);
    Mono<byte[]> download(String path);
}
