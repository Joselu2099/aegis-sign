package com.aegis.sign.domain.port;

import com.aegis.sign.domain.model.Signature;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SignatureRepositoryPort {
    Mono<Signature> save(Signature signature);
    Mono<Signature> findById(UUID id);
}
