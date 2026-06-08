package com.aegis.sign.application.usecase;

import com.aegis.sign.application.ports.in.SignatureUseCase;
import com.aegis.sign.domain.model.AuditTrail;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.AuditTrailRepositoryPort;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import com.aegis.sign.domain.port.SignatureServicePort;
import com.aegis.sign.domain.port.EncryptionPort;
import com.aegis.sign.domain.port.StoragePort;
import com.aegis.sign.domain.service.PdfTemplateCompiler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SignatureInteractor implements SignatureUseCase {

    private final ContractRepositoryPort contractRepositoryPort;
    private final SignatureServicePort signatureServicePort;
    private final SignatureRepositoryPort signatureRepositoryPort;
    private final AuditTrailRepositoryPort auditTrailRepositoryPort;
    private final EncryptionPort encryptionPort;
    private final PdfTemplateCompiler pdfTemplateCompiler;
    private final StoragePort storagePort;

    @Override
    public Mono<String> prepareContractHash(UUID contractId) {
        return contractRepositoryPort.findById(contractId)
                .map(Contract::getContentHash);
    }

    @Override
    public Mono<Signature> getSignature(UUID signatureId) {
        return signatureRepositoryPort.findById(signatureId)
                .switchIfEmpty(Mono.error(new com.aegis.sign.domain.exception.ResourceNotFoundException("Signature not found: " + signatureId)));
    }

    @Override
    public Mono<Signature> signContract(UUID contractId, UUID kycSessionId, String signerId, String certificateThumbprint, String ipAddress, String userAgent) {
        return contractRepositoryPort.findById(contractId)
                .flatMap(contract -> encryptionPort.encrypt(certificateThumbprint)
                        .flatMap(encryptedThumbprint -> signatureServicePort.sign(contract.getContentHash(), encryptedThumbprint)
                                .flatMap(signatureHash -> {
                                    Signature signature = Signature.builder()
                                            .id(UUID.randomUUID())
                                            .contractId(contractId)
                                            .signerId(signerId)
                                            .hash(signatureHash)
                                            .certificateThumbprint(encryptedThumbprint) // Store encrypted thumbprint
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
                        })) // Corrected closing for encryptedThumbprint flatMap
                ); // Corrected closing for contract flatMap
    }

    @Override
    public Mono<byte[]> generateAndSignAuditTrailPdf(UUID contractId) {
        return auditTrailRepositoryPort.findByContractId(contractId)
                .flatMap(auditTrail -> {
                    // Assuming PdfTemplateCompiler.compile expects a map of data
                    // and a template name. We'll need a template for the audit trail.
                    // For now, let's convert the AuditTrail object to a map.
                    Map<String, Object> data = Map.of(
                            "contractId", auditTrail.getContractId().toString(),
                            "kycSessionId", auditTrail.getKycSessionId().toString(),
                            "events", auditTrail.getEvents().stream()
                                    .map(event -> Map.of(
                                            "eventType", event.getEventType(),
                                            "timestamp", event.getTimestamp().toString(),
                                            "description", event.getDescription(),
                                            "ipAddress", event.getIpAddress(),
                                            "userAgent", event.getUserAgent()
                                    ))
                                    .collect(java.util.stream.Collectors.toList())
                    );
                    String templateName = "audit-trail-template"; // This template needs to exist

                    return Mono.fromCallable(() -> pdfTemplateCompiler.compile(templateName, data))
                            .flatMap(unsignedPdf -> signatureServicePort.signPdf(unsignedPdf))
                            .flatMap(signedPdf -> storagePort.upload(signedPdf, "audit-trails/" + contractId.toString() + "-audit-trail.pdf"))
                            .thenReturn(new byte[0]); // Return empty byte array for now, or the actual PDF if needed
                });
    }
}
