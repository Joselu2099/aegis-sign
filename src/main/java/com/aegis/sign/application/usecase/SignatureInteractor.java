package com.aegis.sign.application.usecase;

import com.aegis.sign.application.ports.in.SignatureUseCase;
import com.aegis.sign.domain.model.AuditTrail;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.AuditTrailRepositoryPort;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import com.aegis.sign.domain.port.SignatureServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureInteractor implements SignatureUseCase {

    private final ContractRepositoryPort contractRepositoryPort;
    private final SignatureServicePort signatureServicePort;
    private final SignatureRepositoryPort signatureRepositoryPort;
    private final AuditTrailRepositoryPort auditTrailRepositoryPort;

    @Override
    public Mono<String> prepareContractHash(UUID contractId) {
        return contractRepositoryPort.findById(contractId)
                .map(Contract::getContentHash);
    }

    @Override
    public Mono<Signature> signContract(UUID contractId, UUID kycSessionId, String signerId, String certificateThumbprint, String ipAddress, String userAgent) {
        return contractRepositoryPort.findById(contractId)
                .flatMap(contract -> signatureServicePort.sign(contract.getContentHash(), certificateThumbprint)
                        .flatMap(signatureHash -> {
                            Signature signature = Signature.builder()
                                    .id(UUID.randomUUID())
                                    .contractId(contractId)
                                    .signerId(signerId)
                                    .hash(signatureHash)
                                    .certificateThumbprint(certificateThumbprint)
                                    .timestamp(LocalDateTime.now())
                                    .build();

                            contract.setStatus(Contract.ContractStatus.SIGNED);

                            AuditTrail.AuditTrailEvent event = AuditTrail.AuditTrailEvent.builder()
                                    .eventType("SIGNATURE")
                                    .timestamp(LocalDateTime.now())
                                    .description("Contract signed by " + signerId)
                                    .ipAddress(ipAddress)
                                    .userAgent(userAgent)
                                    .build();

                            AuditTrail auditTrail = AuditTrail.builder()
                                    .id(UUID.randomUUID())
                                    .contractId(contractId)
                                    .kycSessionId(kycSessionId)
                                    .events(List.of(event))
                                    .build();

                            return signatureRepositoryPort.save(signature)
                                    .then(contractRepositoryPort.save(contract))
                                    .then(auditTrailRepositoryPort.save(auditTrail))
                                    .thenReturn(signature);
                        }));
    }
}
