package com.aegis.sign.infrastructure.adapter.storage;

import com.aegis.sign.domain.port.StoragePort;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZonedDateTime;

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

    @Override
    public Flux<String> listOlderThan(int days) {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(days);
        return Flux.defer(() -> {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .recursive(true)
                            .build()
            );
            return Flux.fromIterable(results)
                    .flatMap(result -> {
                        try {
                            Item item = result.get();
                            if (item.lastModified().isBefore(threshold)) {
                                return Mono.just(item.objectName());
                            }
                            return Mono.empty();
                        } catch (Exception e) {
                            log.error("Error listing MinIO objects", e);
                            return Mono.error(e);
                        }
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(String path) {
        return Mono.fromRunnable(() -> {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(path)
                                .build()
                );
                log.info("Deleted expired MinIO object: {}", path);
            } catch (Exception e) {
                log.error("Error deleting MinIO object: {}", path, e);
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
