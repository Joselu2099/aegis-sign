package com.aegis.sign.domain.port;

import com.aegis.sign.domain.model.KycSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface KycRepositoryPort {
    Mono<KycSession> save(KycSession session);
    Mono<KycSession> findById(UUID id);
}
