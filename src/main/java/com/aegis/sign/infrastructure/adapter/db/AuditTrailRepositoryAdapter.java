package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.AuditTrail;
import com.aegis.sign.domain.port.AuditTrailRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.AuditTrailEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.AuditTrailRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private AuditTrailEntity toEntity(AuditTrail auditTrail) {
        String manifest = "[]";
        try {
            manifest = objectMapper.writeValueAsString(auditTrail.getEvents());
        } catch (JsonProcessingException e) {
            // handle
            System.err.println("Error serializing audit trail events: " + e.getMessage());
        }

        return AuditTrailEntity.builder()
                .id(auditTrail.getId())
                .contractId(auditTrail.getContractId())
                .kycSessionId(auditTrail.getKycSessionId())
                .trailManifest(io.r2dbc.postgresql.codec.Json.of(manifest))
                .build();
    }

    private AuditTrail toDomain(AuditTrailEntity entity) {
        List<AuditTrail.AuditTrailEvent> events = Collections.emptyList();
        try {
            if (entity.getTrailManifest() != null) {
                events = objectMapper.readValue(entity.getTrailManifest().asString(), new TypeReference<List<AuditTrail.AuditTrailEvent>>() {});
            }
        } catch (JsonProcessingException e) {
            // handle
            System.err.println("Error deserializing audit trail events: " + e.getMessage());
        }
        return AuditTrail.builder()
                .id(entity.getId())
                .contractId(entity.getContractId())
                .kycSessionId(entity.getKycSessionId())
                .events(events)
                .build();
    }
}
