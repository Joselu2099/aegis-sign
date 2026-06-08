package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.KycSessionEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.KycSessionRepository;
import com.aegis.sign.infrastructure.adapter.web.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KycRepositoryAdapter implements KycRepositoryPort {

    private final KycSessionRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<KycSession> save(KycSession session) {
        return repository.existsById(session.getId())
                .flatMap(exists -> repository.save(toEntity(session, !exists)))
                .map(this::toDomain);
    }

    @Override
    public Mono<KycSession> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("KYC session not found: " + id)));
    }

    private KycSessionEntity toEntity(KycSession session, boolean isNew) {
        String extractedData = null;
        try {
            Map<String, Object> data = new HashMap<>(session.getDocumentMetadata());
            data.put("signerId", session.getSignerId());
            extractedData = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            // Log error or throw runtime exception
        }

        return KycSessionEntity.builder()
                .id(session.getId())
                .status(mapStatusToDb(session.getStatus()))
                .extractedData(extractedData)
                .biometricScore(session.getFaceMatchScore())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .isNew(isNew)
                .build();
    }

    private KycSession toDomain(KycSessionEntity entity) {
        Map<String, String> metadata = new HashMap<>();
        String signerId = null;
        if (entity.getExtractedData() != null) {
            try {
                Map<String, Object> data = objectMapper.readValue(entity.getExtractedData(), new TypeReference<>() {});
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if ("signerId".equals(entry.getKey())) {
                        signerId = (String) entry.getValue();
                    } else {
                        metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            } catch (JsonProcessingException e) {
                // Log error
            }
        }

        return KycSession.builder()
                .id(entity.getId())
                .status(mapStatusToDomain(entity.getStatus()))
                .documentMetadata(metadata)
                .faceMatchScore(entity.getBiometricScore())
                .signerId(signerId)
                .build();
    }

    private String mapStatusToDb(KycSession.KycStatus status) {
        if (status == null) return "PENDING_DOCUMENTS";
        return switch (status) {
            case PENDING_DOCUMENTS -> "PENDING_DOCUMENTS";
            case PROCESSING        -> "PROCESSING";
            case MANUAL_REVIEW     -> "MANUAL_REVIEW";
            case APPROVED          -> "APPROVED";
            case REJECTED          -> "REJECTED";
        };
    }

    private KycSession.KycStatus mapStatusToDomain(String status) {
        if (status == null) return KycSession.KycStatus.PENDING_DOCUMENTS;
        return switch (status) {
            case "PROCESSING"        -> KycSession.KycStatus.PROCESSING;
            case "MANUAL_REVIEW"     -> KycSession.KycStatus.MANUAL_REVIEW;
            case "APPROVED"          -> KycSession.KycStatus.APPROVED;
            case "REJECTED"          -> KycSession.KycStatus.REJECTED;
            default                  -> KycSession.KycStatus.PENDING_DOCUMENTS;
        };
    }
}
