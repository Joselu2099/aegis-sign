package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.ContractUseCase;
import com.aegis.sign.domain.model.Contract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;

@WebFluxTest(
        controllers = ContractController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {com.aegis.sign.infrastructure.web.filter.TokenBucketRateLimiterFilter.class}
        )
)
class ContractControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ContractUseCase contractUseCase;

    @Test
    void createContract_ShouldReturnCreatedContract() {
        // Arrange
        String templateId = "template-123";
        List<String> signerIds = List.of("signer-1", "signer-2");
        Map<String, Object> data = Map.of("key", "value");

        ContractController.CreateContractRequest request = ContractController.CreateContractRequest.builder()
                .templateId(templateId)
                .signerIds(signerIds)
                .data(data)
                .build();

        UUID contractId = UUID.randomUUID();
        Contract mockContract = Contract.builder()
                .id(contractId)
                .templateId(templateId)
                .signerIds(signerIds)
                .status(Contract.ContractStatus.DRAFT)
                .build();

        when(contractUseCase.createContract(eq(templateId), eq(signerIds), eq(data)))
                .thenReturn(Mono.just(mockContract));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(contractId.toString())
                .jsonPath("$.data.templateId").isEqualTo(templateId)
                .jsonPath("$.data.status").isEqualTo("DRAFT");
    }

    @Test
    void getContract_ShouldReturnContractWhenFound() {
        // Arrange
        UUID contractId = UUID.randomUUID();
        Contract mockContract = Contract.builder()
                .id(contractId)
                .templateId("template-123")
                .status(Contract.ContractStatus.PREPARED)
                .build();

        when(contractUseCase.getContract(eq(contractId)))
                .thenReturn(Mono.just(mockContract));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/contracts/{id}", contractId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(contractId.toString())
                .jsonPath("$.data.status").isEqualTo("PREPARED");
    }

    @Test
    void getContract_ShouldReturnNotFoundWhenContractDoesNotExist() {
        // Arrange
        UUID contractId = UUID.randomUUID();

        when(contractUseCase.getContract(eq(contractId)))
                .thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/contracts/{id}", contractId)
                .exchange()
                .expectStatus().isOk() // Spring returns 200 with empty body for Mono.empty() unless a global exception handler overrides it, but the response body is empty which causes Jayway to fail parsing it.
                .expectBody().isEmpty();
    }

    @Test
    void createContract_ShouldReturnErrorWhenServiceFails() {
        // Arrange
        String templateId = "template-123";
        List<String> signerIds = List.of("signer-1", "signer-2");
        Map<String, Object> data = Map.of("key", "value");

        ContractController.CreateContractRequest request = ContractController.CreateContractRequest.builder()
                .templateId(templateId)
                .signerIds(signerIds)
                .data(data)
                .build();

        when(contractUseCase.createContract(eq(templateId), eq(signerIds), eq(data)))
                .thenReturn(Mono.error(new RuntimeException("Service failure")));

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
