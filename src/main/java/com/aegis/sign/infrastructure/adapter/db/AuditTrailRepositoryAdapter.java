package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.AuditTrail;
import com.aegis.sign.domain.port.AuditTrailRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.AuditTrailEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.AuditTrailRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditTrailRepositoryAdapter implements AuditTrailRepositoryPort {

    private final AuditTrailRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<AuditTrail> save(AuditTrail auditTrail) {
        return toEntity(auditTrail)
                .flatMap(repository::save)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<AuditTrail> findById(UUID id) {
        return repository.findById(id)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<AuditTrail> findByContractId(UUID contractId) {
        return repository.findByContractId(contractId)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Void> updateFinalSignedPdfUri(UUID auditTrailId, String uri) {
        return repository.updateFinalSignedPdfUri(auditTrailId, uri);
    }

    private Mono<AuditTrailEntity> toEntity(AuditTrail auditTrail) {
        return Mono.fromCallable(() -> {
            TrailManifest trailManifest = TrailManifest.builder()
                    .events(auditTrail.getEvents())
                    .ocrMrzResults(auditTrail.getOcrMrzResults())
                    .biometricScore(auditTrail.getBiometricScore())
                    .preSignatureHash(auditTrail.getPreSignatureHash())
                    .postSignatureHash(auditTrail.getPostSignatureHash())
                    .build();
            String manifest;
            try {
                manifest = objectMapper.writeValueAsString(trailManifest);
            } catch (JsonProcessingException e) {
                throw new AuditTrailPersistenceException("Failed to serialize audit trail manifest for contract "
                        + auditTrail.getContractId(), e);
            }

            return AuditTrailEntity.builder()
                    .id(auditTrail.getId())
                    .contractId(auditTrail.getContractId())
                    .kycSessionId(auditTrail.getKycSessionId())
                    .trailManifest(io.r2dbc.postgresql.codec.Json.of(manifest))
                    .finalSignedPdfUri(auditTrail.getFinalSignedPdfUri())
                    .build();
        });
    }

    private Mono<AuditTrail> toDomain(AuditTrailEntity entity) {
        return Mono.fromCallable(() -> {
            TrailManifest trailManifest = null;
            if (entity.getTrailManifest() != null) {
                try {
                    trailManifest = objectMapper.readValue(entity.getTrailManifest().asString(), TrailManifest.class);
                } catch (JsonProcessingException e) {
                    throw new AuditTrailPersistenceException("Failed to deserialize audit trail manifest for audit trail "
                            + entity.getId(), e);
                }
            }

            List<AuditTrail.AuditTrailEvent> events = trailManifest != null && trailManifest.getEvents() != null
                    ? trailManifest.getEvents()
                    : Collections.emptyList();

            AuditTrail.AuditTrailBuilder builder = AuditTrail.builder()
                    .id(entity.getId())
                    .contractId(entity.getContractId())
                    .kycSessionId(entity.getKycSessionId())
                    .events(events)
                    .finalSignedPdfUri(entity.getFinalSignedPdfUri());

            if (trailManifest != null) {
                builder.ocrMrzResults(trailManifest.getOcrMrzResults())
                        .biometricScore(trailManifest.getBiometricScore())
                        .preSignatureHash(trailManifest.getPreSignatureHash())
                        .postSignatureHash(trailManifest.getPostSignatureHash());
            }

            return builder.build();
        });
    }

    /**
     * Thrown when the {@code trail_manifest} JSON payload cannot be
     * serialized or deserialized. Audit trail data is legal evidence, so a
     * (de)serialization failure must fail the reactive chain rather than
     * silently persisting or returning a corrupted/incomplete record.
     */
    static class AuditTrailPersistenceException extends RuntimeException {
        AuditTrailPersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * JSON shape persisted into the {@code trail_manifest} JSONB column.
     * Intended to hold the complete log of cryptographic events and
     * verification scores (events, OCR/MRZ results, biometric score, and
     * both pre/post signature hashes) per the documented purpose of
     * trail_manifest in docs/database.md.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TrailManifest {
        private List<AuditTrail.AuditTrailEvent> events;
        private String ocrMrzResults;
        private Double biometricScore;
        private String preSignatureHash;
        private String postSignatureHash;
    }
}
