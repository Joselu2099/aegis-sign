package com.aegis.sign.infrastructure.adapter.storage;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MinioStorageAdapterMockTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioStorageAdapter minioStorageAdapter;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(minioStorageAdapter, "bucket", "aegis-sign");
        org.springframework.test.util.ReflectionTestUtils.setField(minioStorageAdapter, "tempBucket", "aegis-sign-temp");
    }

    @Test
    void delete_ExistingObject_ShouldRemoveObject() throws Exception {
        String testPath = "test-folder/test-file.txt";

        StepVerifier.create(minioStorageAdapter.delete(testPath))
                .verifyComplete();

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());

        RemoveObjectArgs args = captor.getValue();
        assertEquals("aegis-sign", args.bucket());
        assertEquals(testPath, args.object());
    }

    @Test
    void deleteTempFile_ExistingObject_ShouldRemoveObject() throws Exception {
        String testPath = "temp-folder/temp-file.txt";

        StepVerifier.create(minioStorageAdapter.deleteTempFile(testPath))
                .verifyComplete();

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());

        RemoveObjectArgs args = captor.getValue();
        assertEquals("aegis-sign-temp", args.bucket());
        assertEquals(testPath, args.object());
    }
}
