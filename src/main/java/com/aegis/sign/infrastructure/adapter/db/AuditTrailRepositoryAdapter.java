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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditTrailRepositoryAdapter implements AuditTrailRepositoryPort {

    private final AuditTrailRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<AuditTrail> save(AuditTrail auditTrail) {
        return repository.save(toEntity(auditTrail))
                .map(this::toDomain);
    }

    @Override
    public Mono<AuditTrail> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<AuditTrail> findByContractId(UUID contractId) {
        return repository.findByContractId(contractId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> updateFinalSignedPdfUri(UUID auditTrailId, String uri) {
        return repository.updateFinalSignedPdfUri(auditTrailId, uri);
    }

    private AuditTrailEntity toEntity(AuditTrail auditTrail) {
        String manifest = "{}";
        try {
            TrailManifest trailManifest = TrailManifest.builder()
                    .events(auditTrail.getEvents())
                    .ocrMrzResults(auditTrail.getOcrMrzResults())
                    .biometricScore(auditTrail.getBiometricScore())
                    .preSignatureHash(auditTrail.getPreSignatureHash())
                    .postSignatureHash(auditTrail.getPostSignatureHash())
                    .build();
            manifest = objectMapper.writeValueAsString(trailManifest);
        } catch (JsonProcessingException e) {
            log.error("Error serializing audit trail manifest", e);
        }

        return AuditTrailEntity.builder()
                .id(auditTrail.getId())
                .contractId(auditTrail.getContractId())
                .kycSessionId(auditTrail.getKycSessionId())
                .trailManifest(io.r2dbc.postgresql.codec.Json.of(manifest))
                .finalSignedPdfUri(auditTrail.getFinalSignedPdfUri())
                .build();
    }

    private AuditTrail toDomain(AuditTrailEntity entity) {
        TrailManifest trailManifest = null;
        try {
            if (entity.getTrailManifest() != null) {
                trailManifest = objectMapper.readValue(entity.getTrailManifest().asString(), TrailManifest.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing audit trail manifest", e);
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
    }

    /**
     * JSON shape persisted into the {@code trail_manifest} JSONB column.
     * Holds the complete log of cryptographic events and verification scores,
     * as documented in docs/database.md.
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
