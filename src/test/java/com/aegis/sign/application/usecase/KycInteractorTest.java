package com.aegis.sign.application.usecase;

import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycInteractorTest {

    @Mock
    private KycRepositoryPort kycRepositoryPort;

    private KycInteractor kycInteractor;

    @BeforeEach
    void setUp() {
        kycInteractor = new KycInteractor(kycRepositoryPort);
    }

    @Test
    void verifySession_ShouldSetStatusToApproved() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(sessionId)
                .status(KycSession.KycStatus.PENDING)
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

        when(kycRepositoryPort.findById(sessionId)).thenReturn(Mono.just(session));
        when(kycRepositoryPort.save(any(KycSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<KycSession> result = kycInteractor.submitIdDocument(sessionId, new byte[]{1, 2, 3});

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(s -> "UPLOADED".equals(s.getDocumentMetadata().get("ID_DOCUMENT")))
                .verifyComplete();
    }

    @Test
    void submitBiometrics_ShouldUpdateMetadata() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(sessionId)
                .documentMetadata(new HashMap<>())
                .build();

        when(kycRepositoryPort.findById(sessionId)).thenReturn(Mono.just(session));
        when(kycRepositoryPort.save(any(KycSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<KycSession> result = kycInteractor.submitBiometrics(sessionId, new byte[]{4, 5, 6});

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(s -> "UPLOADED".equals(s.getDocumentMetadata().get("BIOMETRICS")))
                .verifyComplete();
    }
}
