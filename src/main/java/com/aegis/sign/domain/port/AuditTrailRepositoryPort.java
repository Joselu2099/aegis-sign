package com.aegis.sign.domain.port;

import com.aegis.sign.domain.model.AuditTrail;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuditTrailRepositoryPort {
    Mono<AuditTrail> save(AuditTrail auditTrail);
    Mono<AuditTrail> findById(UUID id);
    Mono<AuditTrail> findByContractId(UUID contractId);
    Mono<Void> updateFinalSignedPdfUri(UUID auditTrailId, String uri);
}
