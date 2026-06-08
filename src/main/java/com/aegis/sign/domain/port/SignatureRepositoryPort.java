package com.aegis.sign.domain.port;

import com.aegis.sign.domain.model.Signature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SignatureRepositoryPort {
    Mono<Signature> save(Signature signature);
    Mono<Signature> findById(UUID id);
    Mono<Page<Signature>> findByContractId(UUID contractId, Pageable pageable);
}
