package com.aegis.sign.application.ports.in;

import com.aegis.sign.domain.model.KycSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface KycUseCase {
    Mono<KycSession> createSession(String signerId);
    Mono<KycSession> verifySession(UUID sessionId);
    Mono<KycSession> submitIdDocument(UUID sessionId, byte[] content);
    Mono<KycSession> submitBiometrics(UUID sessionId, byte[] content);
}
