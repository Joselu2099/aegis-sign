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
    void purgeExpiredFiles_ShouldCallListAndDelete() {
        // Given
        String path1 = "path/to/old/file1.png";
        String path2 = "path/to/old/file2.pdf";
        when(storagePort.listOlderThan(7)).thenReturn(Flux.just(path1, path2));
        when(storagePort.delete(anyString())).thenReturn(Mono.empty());

        // When
        storagePurgeWorker.purgeExpiredFiles();

        // Then
        verify(storagePort).listOlderThan(7);
        verify(storagePort).delete(path1);
        verify(storagePort).delete(path2);
    }

    @Test
    void purgeExpiredFiles_WhenEmpty_ShouldNotDelete() {
        // Given
        when(storagePort.listOlderThan(7)).thenReturn(Flux.empty());

        // When
        storagePurgeWorker.purgeExpiredFiles();

        // Then
        verify(storagePort).listOlderThan(7);
        verify(storagePort, never()).delete(anyString());
    }
}
