package com.aegis.sign.application.usecase;

import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import com.aegis.sign.domain.port.StoragePort;
import com.aegis.sign.domain.service.MrzValidationService;
import com.aegis.sign.domain.service.OcrExtractorService;
import com.aegis.sign.domain.service.BiometricValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString; // Import anyString
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycInteractorTest {

    @Mock
    private KycRepositoryPort kycRepositoryPort;
    @Mock
    private StoragePort storagePort; // Mock StoragePort
    @Mock
    private OcrExtractorService ocrExtractorService;
    @Mock
    private MrzValidationService mrzValidationService;
    @Mock
    private BiometricValidationService biometricValidationService;

    private KycInteractor kycInteractor;

    @BeforeEach
    void setUp() {
        kycInteractor = new KycInteractor(kycRepositoryPort, storagePort, ocrExtractorService, mrzValidationService, biometricValidationService); // Inject all dependencies
    }

    @Test
    void verifySession_ShouldSetStatusToApproved() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(sessionId)
                .status(KycSession.KycStatus.PENDING_DOCUMENTS)
                .build();

        when(kycRepositoryPort.findById(sessionId)).thenReturn(Mono.just(session));
        when(kycRepositoryPort.save(any(KycSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<KycSession> result = kycInteractor.verifySession(sessionId);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(s -> s.getStatus() == KycSession.KycStatus.APPROVED)
                .verifyComplete();

        verify(kycRepositoryPort).save(argThat(s -> s.getStatus() == KycSession.KycStatus.APPROVED));
    }

    @Test
    void submitIdDocument_ShouldUpdateMetadata() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(sessionId)
                .documentMetadata(new HashMap<>())
                .build();

        Map<String, String> mockOcrData = new HashMap<>();
        mockOcrData.put("mrzType", "TD3");
        mockOcrData.put("documentNumber", "123");
        mockOcrData.put("documentNumberCheckDigit", "1");
        mockOcrData.put("birthDate", "900101");
        mockOcrData.put("birthDateCheckDigit", "1");
        mockOcrData.put("expiryDate", "300101");
        mockOcrData.put("expiryDateCheckDigit", "1");
        mockOcrData.put("mrzLine1", "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<");
        mockOcrData.put("mrzLine2", "L898902C<3UTO8001015F2405236<<<<<<<<<<<<<<0");
        mockOcrData.put("compositeCheckDigit", "0");

        when(kycRepositoryPort.findById(sessionId)).thenReturn(Mono.just(session));
        when(kycRepositoryPort.save(any(KycSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(ocrExtractorService.extractData(any(byte[].class))).thenReturn(mockOcrData);
        when(mrzValidationService.validateChecksum(anyString(), anyChar())).thenReturn(true);

        // Act
        Mono<KycSession> result = kycInteractor.submitIdDocument(sessionId, new byte[]{1, 2, 3});

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(s -> "UPLOADED".equals(s.getDocumentMetadata().get("ID_DOCUMENT")))
                .verifyComplete();
    }

    @Test
    void submitBiometrics_ShouldUploadFileAndStorePath() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        byte[] biometricContent = {4, 5, 6};
        String expectedPath = "biometrics/" + sessionId.toString() + "/some-uuid"; // dummy path

        KycSession session = KycSession.builder()
                .id(sessionId)
                .documentMetadata(new HashMap<>())
                .build();

        BiometricValidationService.ValidationResult validationResult = BiometricValidationService.ValidationResult.builder()
                .isValid(true)
                .contrast(1.0)
                .width(100)
                .height(100)
                .livenessScore(0.9)
                .faceDetected(true)
                .build();

        when(kycRepositoryPort.findById(sessionId)).thenReturn(Mono.just(session));
        when(biometricValidationService.validate(any(byte[].class))).thenReturn(validationResult);
        when(storagePort.uploadTempFile(any(byte[].class), anyString())).thenReturn(Mono.just(expectedPath)); // Mock upload
        when(kycRepositoryPort.save(any(KycSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<KycSession> result = kycInteractor.submitBiometrics(sessionId, biometricContent);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(s -> expectedPath.equals(s.getDocumentMetadata().get("BIOMETRICS")))
                .verifyComplete();

        verify(storagePort).uploadTempFile(eq(biometricContent), anyString()); // Verify upload was called
        verify(kycRepositoryPort).save(argThat(s -> expectedPath.equals(s.getDocumentMetadata().get("BIOMETRICS"))));
    }
}
