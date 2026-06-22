package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.AbstractIntegrationTest;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SignatureControllerE2ETest extends AbstractIntegrationTest {

    @Autowired
    private SignatureRepositoryPort signatureRepositoryPort;

    @Autowired
    private ContractRepositoryPort contractRepositoryPort;

    private Contract contract;
    private Signature signature1;
    private Signature signature2;

    @BeforeEach
    void setUp() {
        setupWebTestClient();

        // Seed contract
        contract = Contract.builder()
                .templateId("template-1")
                .status(Contract.ContractStatus.PREPARED)
                .contentHash("hash-abc123")
                .uri("s3://bucket/contract.pdf")
                .signerIds(Collections.singletonList("signer-001"))
                .build();
        contract = contractRepositoryPort.save(contract).block();
        assertNotNull(contract);

        // Seed signatures
        signature1 = Signature.builder()
                .contractId(contract.getId())
                .signerId("signer-001")
                .hash("sig-hash-1")
                .certificateThumbprint("thumb-1")
                .timestamp(LocalDateTime.now())
                .build();
        signature1 = signatureRepositoryPort.save(signature1).block();

        signature2 = Signature.builder()
                .contractId(contract.getId())
                .signerId("signer-002")
                .hash("sig-hash-2")
                .certificateThumbprint("thumb-2")
                .timestamp(LocalDateTime.now())
                .build();
        signature2 = signatureRepositoryPort.save(signature2).block();
    }

    @Test
    void getSignatureById_ShouldReturn200WithData() {
        webTestClient.get()
                .uri("/api/v1/signatures/{id}", signature1.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(signature1.getId().toString())
                .jsonPath("$.data.signerId").isEqualTo("signer-001")
                .jsonPath("$.data.contractId").isEqualTo(contract.getId().toString());
    }

    @Test
    void getSignatureById_ShouldReturn404WhenNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        webTestClient.get()
                .uri("/api/v1/signatures/{id}", nonExistentId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }

    @Test
    void listSignaturesByContractId_ShouldReturn200WithPaginatedResults() {
        webTestClient.get()
                .uri("/api/v1/signatures?contractId={id}&page=0&size=20", contract.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.totalElements").isEqualTo(2)
                .jsonPath("$.data.content[0].id").isNotEmpty()
                .jsonPath("$.data.content[0].contractId").isEqualTo(contract.getId().toString())
                .jsonPath("$.data.content[1].id").isNotEmpty();
    }

    @Test
    void listSignaturesByContractId_ShouldReturn200EmptyWhenNoSignatures() {
        UUID otherContractId = UUID.randomUUID();
        webTestClient.get()
                .uri("/api/v1/signatures?contractId={id}&page=0&size=20", otherContractId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.totalElements").isEqualTo(0)
                .jsonPath("$.data.content").isArray()
                .jsonPath("$.data.content.length()").isEqualTo(0);
    }

    @Test
    void listSignaturesByContractId_ShouldRespectPageSize() {
        // Create a third signature
        Signature signature3 = Signature.builder()
                .contractId(contract.getId())
                .signerId("signer-003")
                .hash("sig-hash-3")
                .certificateThumbprint("thumb-3")
                .timestamp(LocalDateTime.now())
                .build();
        signatureRepositoryPort.save(signature3).block();

        // Request page 0 with size 2
        webTestClient.get()
                .uri("/api/v1/signatures?contractId={id}&page=0&size=2", contract.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.totalElements").isEqualTo(3)
                .jsonPath("$.data.content.length()").isEqualTo(2)
                .jsonPath("$.data.number").isEqualTo(0)
                .jsonPath("$.data.size").isEqualTo(2);
    }

    @Test
    void listSignaturesByContractId_ShouldReturnDefaultPageSize() {
        webTestClient.get()
                .uri("/api/v1/signatures?contractId={id}", contract.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.size").isEqualTo(20); // default page size
    }
}
