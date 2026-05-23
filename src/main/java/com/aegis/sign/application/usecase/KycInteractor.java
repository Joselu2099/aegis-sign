package com.aegis.sign.application.usecase;

import com.aegis.sign.application.ports.in.KycUseCase;
import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycInteractor implements KycUseCase {

    private final KycRepositoryPort kycRepositoryPort;

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
    public Mono<KycSession> uploadDocument(UUID sessionId, String documentType, byte[] content) {
        return kycRepositoryPort.findById(sessionId)
                .flatMap(session -> {
                    // Logic for document upload and OCR extraction
                    // In a real scenario, we would use StoragePort and OcrExtractorService
                    session.getDocumentMetadata().put(documentType, "UPLOADED");
                    return kycRepositoryPort.save(session);
                });
    }
}
