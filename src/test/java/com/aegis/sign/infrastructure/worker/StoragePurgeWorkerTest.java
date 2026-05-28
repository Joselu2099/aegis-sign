package com.aegis.sign.infrastructure.worker;

import com.aegis.sign.domain.port.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoragePurgeWorkerTest {

    @Mock
    private StoragePort storagePort;

    @InjectMocks
    private StoragePurgeWorker storagePurgeWorker;

    @Test
    void purgeExpiredFiles_ShouldCallListAndDeleteTempFiles() {
        // Given
        org.springframework.test.util.ReflectionTestUtils.setField(storagePurgeWorker, "retentionDays", 7);
        String path1 = "path/to/old/file1.png";
        String path2 = "path/to/old/file2.pdf";
        when(storagePort.listTempFilesOlderThan(7)).thenReturn(Flux.just(path1, path2));
        when(storagePort.deleteTempFile(anyString())).thenReturn(Mono.empty());

        // When
        storagePurgeWorker.purgeExpiredFiles();

        // Then
        verify(storagePort).listTempFilesOlderThan(7);
        verify(storagePort).deleteTempFile(path1);
        verify(storagePort).deleteTempFile(path2);
    }

    @Test
    void purgeExpiredFiles_WhenEmpty_ShouldNotDeleteAnyTempFiles() {
        // Given
        org.springframework.test.util.ReflectionTestUtils.setField(storagePurgeWorker, "retentionDays", 7);
        when(storagePort.listTempFilesOlderThan(7)).thenReturn(Flux.empty());

        // When
        storagePurgeWorker.purgeExpiredFiles();

        // Then
        verify(storagePort).listTempFilesOlderThan(7);
        verify(storagePort, never()).deleteTempFile(anyString());
    }
}
