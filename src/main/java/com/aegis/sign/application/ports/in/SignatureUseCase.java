package com.aegis.sign.application.ports.in;

import com.aegis.sign.domain.model.Signature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SignatureUseCase {
    Mono<String> prepareContractHash(UUID contractId);
    Mono<Signature> getSignature(UUID signatureId);
    Mono<Page<Signature>> listByContractId(UUID contractId, Pageable pageable);
    Mono<Signature> signContract(UUID contractId, UUID kycSessionId, String signerId, String certificateThumbprint, String ipAddress, String userAgent);
    Mono<byte[]> generateAndSignAuditTrailPdf(UUID contractId);
}
