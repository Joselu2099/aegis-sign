package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.AbstractIntegrationTest;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.KycRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ContractRepositoryPort contractRepositoryPort;

    @Autowired
    private KycRepositoryPort kycRepositoryPort;

    @BeforeEach
    void setUp() {
        setupWebTestClient();
    }

    @Test
    void signContract_ShouldReturnSuccess() {
        // 1. Seed KYC Session
        KycSession kycSession = KycSession.builder()
                .status(KycSession.KycStatus.APPROVED)
                .documentMetadata(Collections.emptyMap())
                .signerId("signer123")
                .build();
        KycSession savedKyc = kycRepositoryPort.save(kycSession).block();
        UUID kycSessionId = savedKyc.getId();

        // 2. Seed Contract
        Contract contract = Contract.builder()
                .templateId("template-1")
                .status(Contract.ContractStatus.PREPARED)
                .contentHash("hash-123")
                .uri("s3://bucket/document.pdf")
                .build();
        Contract savedContract = contractRepositoryPort.save(contract).block();
        UUID contractId = savedContract.getId();

        // 3. Call Sign Endpoint
        SignatureController.SignRequest request = SignatureController.SignRequest.builder()
                .contractId(contractId)
                .kycSessionId(kycSessionId)
                .signerId("signer123")
                .certificateThumbprint("cert-thumbprint")
                .build();

        webTestClient.post()
                .uri("/api/v1/signatures/sign")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.contractId").isEqualTo(contractId.toString());

        // 4. Verify DB state
        Contract updatedContract = contractRepositoryPort.findById(contractId).block();
        assertTrue(updatedContract.getStatus() == Contract.ContractStatus.SIGNED);
    }
}
