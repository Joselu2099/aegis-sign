package com.aegis.sign.infrastructure.adapter.db.repository;

import com.aegis.sign.infrastructure.adapter.db.entity.SignatureEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
 
import java.util.UUID;
 
@Repository
public interface SignatureRepository extends ReactiveCrudRepository<SignatureEntity, UUID> {
    Flux<SignatureEntity> findByContractId(UUID contractId, Pageable pageable);
    Mono<Long> countByContractId(UUID contractId);
}
