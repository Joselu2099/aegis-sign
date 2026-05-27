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

    @Value("${minio.temp-bucket}")
    private String tempBucket;

    private Mono<String> uploadToBucket(byte[] content, String path, String targetBucket) {
        return Mono.fromCallable(() -> {
            try (InputStream is = new ByteArrayInputStream(content)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(targetBucket)
                                .object(path)
                                .stream(is, (long) content.length, -1L)
                                .build()
                );
                return path;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Flux<String> listObjectsOlderThanFromBucket(int days, String targetBucket) {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(days);
        return Flux.defer(() -> {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(targetBucket)
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
                            log.error("Error listing MinIO objects from bucket {}: {}", targetBucket, e.getMessage());
                            return Mono.error(e);
                        }
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> deleteObjectFromBucket(String path, String targetBucket) {
        return Mono.fromRunnable(() -> {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(targetBucket)
                                .object(path)
                                .build()
                );
                log.info("Deleted expired MinIO object: {} from bucket {}", path, targetBucket);
            } catch (Exception e) {
                log.error("Error deleting Minio object: {} from bucket {}: {}", path, targetBucket, e.getMessage());
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<String> upload(byte[] content, String path) {
        return uploadToBucket(content, path, bucket);
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
        return listObjectsOlderThanFromBucket(days, bucket);
    }

    @Override
    public Mono<Void> delete(String path) {
        return deleteObjectFromBucket(path, bucket);
    }

    @Override
    public Mono<String> uploadTempFile(byte[] content, String path) {
        return uploadToBucket(content, path, tempBucket);
    }

    @Override
    public Flux<String> listTempFilesOlderThan(int days) {
        return listObjectsOlderThanFromBucket(days, tempBucket);
    }

    @Override
    public Mono<Void> deleteTempFile(String path) {
        return deleteObjectFromBucket(path, tempBucket);
    }
}
