package com.aegis.sign.infrastructure.adapter.db.repository;

import com.aegis.sign.infrastructure.adapter.db.entity.AuditTrailEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AuditTrailRepository extends ReactiveCrudRepository<AuditTrailEntity, UUID> {
    Mono<AuditTrailEntity> findByContractId(UUID contractId);

    @Modifying
    @Query("UPDATE audit_trails SET final_signed_pdf_uri = :uri WHERE id = :id")
    Mono<Void> updateFinalSignedPdfUri(@Param("id") UUID id, @Param("uri") String uri);
}
