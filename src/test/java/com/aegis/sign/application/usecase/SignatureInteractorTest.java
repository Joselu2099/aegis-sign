package com.aegis.sign.application.usecase;

import com.aegis.sign.domain.model.AuditTrail;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.AuditTrailRepositoryPort;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import com.aegis.sign.domain.port.SignatureServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignatureInteractorTest {

    @Mock
    private ContractRepositoryPort contractRepositoryPort;
    @Mock
    private SignatureServicePort signatureServicePort;
    @Mock
    private SignatureRepositoryPort signatureRepositoryPort;
    @Mock
    private AuditTrailRepositoryPort auditTrailRepositoryPort;

    private SignatureInteractor signatureInteractor;

    @BeforeEach
    void setUp() {
        signatureInteractor = new SignatureInteractor(
                contractRepositoryPort,
                signatureServicePort,
                signatureRepositoryPort,
                auditTrailRepositoryPort
        );
    }

    @Test
    void signContract_ShouldUpdateStatusAndCreateAuditTrail() {
        // Arrange
        UUID contractId = UUID.randomUUID();
        UUID kycSessionId = UUID.randomUUID();
        String signerId = "user123";
        String certThumbprint = "thumbprint";
        String ip = "127.0.0.1";
        String ua = "Mozilla/5.0";
        String contentHash = "hash123";
        String signatureHash = "sigHash123";

        Contract contract = Contract.builder()
                .id(contractId)
                .contentHash(contentHash)
                .status(Contract.ContractStatus.PREPARED)
                .build();

        when(contractRepositoryPort.findById(contractId)).thenReturn(Mono.just(contract));
        when(signatureServicePort.sign(contentHash, certThumbprint)).thenReturn(Mono.just(signatureHash));
        when(signatureRepositoryPort.save(any(Signature.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(contractRepositoryPort.save(any(Contract.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(auditTrailRepositoryPort.save(any(AuditTrail.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act
        Mono<Signature> result = signatureInteractor.signContract(contractId, kycSessionId, signerId, certThumbprint, ip, ua);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(sig -> {
                    return sig.getContractId().equals(contractId) &&
                           sig.getSignerId().equals(signerId) &&
                           sig.getHash().equals(signatureHash);
                })
                .verifyComplete();

        verify(contractRepositoryPort).save(argThat(c -> c.getStatus() == Contract.ContractStatus.SIGNED));
        verify(auditTrailRepositoryPort).save(argThat(at -> 
            at.getContractId().equals(contractId) && 
            at.getKycSessionId().equals(kycSessionId) &&
            at.getEvents().size() == 1 &&
            at.getEvents().get(0).getIpAddress().equals(ip) &&
            at.getEvents().get(0).getUserAgent().equals(ua)
        ));
    }
}
