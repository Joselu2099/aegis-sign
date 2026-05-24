package com.aegis.sign.infrastructure.worker;

import com.aegis.sign.domain.port.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoragePurgeWorker {

    private final StoragePort storagePort;

    /**
     * Purges files older than 7 days from MinIO.
     * Runs every day at 2:00 AM by default.
     */
    @Scheduled(cron = "${storage.purge.cron:0 0 2 * * *}")
    public void purgeExpiredFiles() {
        log.info("Starting storage purge worker...");
        
        storagePort.listOlderThan(7)
                .flatMap(path -> {
                    log.info("Found expired file: {}, purging...", path);
                    return storagePort.delete(path)
                            .then(Mono.just(path));
                })
                .doOnError(e -> log.error("Error during storage purge", e))
                .doOnComplete(() -> log.info("Storage purge process finished."))
                .subscribe();
    }
}
