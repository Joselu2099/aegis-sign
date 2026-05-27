package com.aegis.sign.infrastructure.worker;

import com.aegis.sign.domain.port.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoragePurgeWorker {

    private final StoragePort storagePort;

    @Value("${storage.purge.retention-days:7}")
    private int retentionDays;

    /**
     * Purges files older than the configured retention days from the temporary MinIO bucket.
     * Runs every day at 2:00 AM by default, configurable via storage.purge.cron.
     */
    @Scheduled(cron = "${storage.purge.cron:0 0 2 * * *}")
    public void purgeExpiredFiles() {
        log.info("Starting storage purge worker for temporary files. Retention days: {}", retentionDays);
        
        storagePort.listTempFilesOlderThan(retentionDays)
                .flatMap(path -> {
                    log.info("Found expired temporary file: {}, purging...", path);
                    return storagePort.deleteTempFile(path)
                            .then(Mono.just(path));
                })
                .doOnError(e -> log.error("Error during temporary storage purge", e))
                .doOnComplete(() -> log.info("Temporary storage purge process finished."))
                .subscribe();
    }
}
