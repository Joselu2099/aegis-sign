package com.aegis.sign.application.usecase;

import com.aegis.sign.application.ports.in.KycUseCase;
import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import com.aegis.sign.domain.port.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycInteractor implements KycUseCase {

    private final KycRepositoryPort kycRepositoryPort;
    private final StoragePort storagePort; // Inject StoragePort

    @Override
    public Mono<KycSession> createSession(String signerId) {
        KycSession session = KycSession.builder()
                .id(UUID.randomUUID())
                .signerId(signerId)
                .status(KycSession.KycStatus.PENDING)
                .documentMetadata(new java.util.HashMap<>())
                .build();
        return kycRepositoryPort.save(session);
    }

    @Override
    public Mono<KycSession> verifySession(UUID sessionId) {
        return kycRepositoryPort.findById(sessionId)
                .flatMap(session -> {
                    // Logic for session verification lifecycle
                    // For now, we simulate approval if session exists
                    session.setStatus(KycSession.KycStatus.APPROVED);
                    return kycRepositoryPort.save(session);
                });
    }

    @Override
    public Mono<KycSession> submitIdDocument(UUID sessionId, byte[] content) {
        return kycRepositoryPort.findById(sessionId)
                .flatMap(session -> {
                    // Specific logic for ID document ingestion
                    session.getDocumentMetadata().put("ID_DOCUMENT", "UPLOADED");
                    return kycRepositoryPort.save(session);
                });
    }

    @Override
    public Mono<KycSession> submitBiometrics(UUID sessionId, byte[] content) {
        return kycRepositoryPort.findById(sessionId)
                .flatMap(session -> {
                    String biometricFilePath = "biometrics/" + sessionId.toString() + "/" + UUID.randomUUID().toString();
                    return storagePort.uploadTempFile(content, biometricFilePath)
                            .flatMap(path -> {
                                session.getDocumentMetadata().put("BIOMETRICS", path);
                                return kycRepositoryPort.save(session);
                            });
                });
    }
}
