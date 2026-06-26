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
import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import com.aegis.sign.domain.service.PdfTemplateCompiler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final KycRepositoryPort kycRepositoryPort;

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
    public Mono<Page<Signature>> listByContractId(UUID contractId, Pageable pageable) {
        return signatureRepositoryPort.findByContractId(contractId, pageable);
    }

    @Override
    public Mono<Signature> signContract(UUID contractId, UUID kycSessionId, String signerId, String certificateThumbprint, String ipAddress, String userAgent) {
        return contractRepositoryPort.findById(contractId)
                .flatMap(contract -> Mono.zip(
                        encryptionPort.encrypt(certificateThumbprint),
                        kycRepositoryPort.findById(kycSessionId)
                )
                        .flatMap(tuple -> {
                            String encryptedThumbprint = tuple.getT1();
                            KycSession kycSession = tuple.getT2();
                            String preSignatureHash = contract.getContentHash();

                            return signatureServicePort.sign(preSignatureHash, encryptedThumbprint)
                                    .flatMap(postSignatureHash -> {
                                        Signature signature = Signature.builder()
                                                .id(UUID.randomUUID())
                                                .contractId(contractId)
                                                .signerId(signerId)
                                                .hash(postSignatureHash)
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
                                                .ocrMrzResults("MRZ Valid: " + kycSession.isMrzValid() + ", Message: " + kycSession.getMrzValidationErrorMessage() + ", Metadata: " + kycSession.getDocumentMetadata()) // Constructed from available fields
                                                .biometricScore(kycSession.getFaceMatchScore()) // Corrected method call
                                                .preSignatureHash(preSignatureHash)
                                                .postSignatureHash(postSignatureHash)
                                                .events(List.of(event))
                                                .build();

                                        return signatureRepositoryPort.save(signature)
                                                .then(contractRepositoryPort.save(contract))
                                                .then(auditTrailRepositoryPort.save(auditTrail))
                                                .thenReturn(signature);
                                    });
                        }));
    }

    @Override
    public Mono<byte[]> generateAndSignAuditTrailPdf(UUID contractId) {
        String jsonTemplate;
        try {
            jsonTemplate = new String(getClass().getClassLoader().getResourceAsStream("audit-trail-template.json").readAllBytes());
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to load audit-trail-template.json", e));
        }

        return auditTrailRepositoryPort.findByContractId(contractId)
                .switchIfEmpty(Mono.error(new com.aegis.sign.domain.exception.ResourceNotFoundException("Audit trail not found for contract: " + contractId)))
                .flatMap(auditTrail -> {
                    Map<String, Object> data = Map.of(
                            "contractId", auditTrail.getContractId().toString(),
                            "kycSessionId", auditTrail.getKycSessionId().toString(),
                            "ocrMrzResults", auditTrail.getOcrMrzResults() != null ? auditTrail.getOcrMrzResults() : "N/A",
                            "biometricScore", auditTrail.getBiometricScore() != null ? auditTrail.getBiometricScore().toString() : "N/A",
                            "preSignatureHash", auditTrail.getPreSignatureHash() != null ? auditTrail.getPreSignatureHash() : "N/A",
                            "postSignatureHash", auditTrail.getPostSignatureHash() != null ? auditTrail.getPostSignatureHash() : "N/A",
                            "events", auditTrail.getEvents().stream()
                                    .map(event -> String.format("Type: %s, Timestamp: %s, Description: %s, IP: %s, UserAgent: %s",
                                            event.getEventType(),
                                            event.getTimestamp().toString(),
                                            event.getDescription(),
                                            event.getIpAddress(),
                                            event.getUserAgent()))
                                    .collect(java.util.stream.Collectors.joining("\n"))
                    );

                    return Mono.fromCallable(() -> pdfTemplateCompiler.compile(jsonTemplate, data))
                            .flatMap(unsignedPdf -> signatureServicePort.signPdf(unsignedPdf))
                            .flatMap(signedPdf -> storagePort.upload(signedPdf, "audit-trails/" + contractId.toString() + "-audit-trail.pdf")
                                    .flatMap(uri -> auditTrailRepositoryPort.updateFinalSignedPdfUri(auditTrail.getId(), uri))
                                    .thenReturn(signedPdf));
                });
    }
}
