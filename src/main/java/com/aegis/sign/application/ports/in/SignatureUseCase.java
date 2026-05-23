package com.aegis.sign.application.ports.in;

import com.aegis.sign.domain.model.Signature;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SignatureUseCase {
    Mono<String> prepareContractHash(UUID contractId);
    Mono<Signature> signContract(UUID contractId, UUID kycSessionId, String signerId, String certificateThumbprint, String ipAddress, String userAgent);
}
