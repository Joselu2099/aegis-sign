package com.aegis.sign.infrastructure.adapter.storage;

import com.aegis.sign.domain.port.StoragePort;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageAdapter implements StoragePort {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public Mono<String> upload(byte[] content, String path) {
        return Mono.fromCallable(() -> {
            try (InputStream is = new ByteArrayInputStream(content)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(path)
                                .stream(is, (long) content.length, -1L)
                                .build()
                );
                return path;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<byte[]> download(String path) {
        return Mono.fromCallable(() -> {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            )) {
                return stream.readAllBytes();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
