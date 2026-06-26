package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.SignatureUseCase;
import com.aegis.sign.application.usecase.SignatureInteractor;
import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.AuditTrailRepositoryPort;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.port.EncryptionPort;
import com.aegis.sign.domain.port.KycRepositoryPort;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import com.aegis.sign.domain.port.SignatureServicePort;
import com.aegis.sign.domain.port.StoragePort;
import com.aegis.sign.domain.service.PdfTemplateCompiler;
import com.aegis.sign.infrastructure.adapter.web.filter.TokenBucketRateLimiterFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
    controllers = SignatureController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = TokenBucketRateLimiterFilter.class
    )
)
class SignatureControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SignatureUseCase signatureUseCase;

    @Test
    void sign_ShouldExtractRealClientIp_NotXForwardedFor() {
        // Arrange
        UUID contractId = UUID.randomUUID();
        UUID kycSessionId = UUID.randomUUID();
        String signerId = "signer-123";
        String certThumbprint = "thumbprint-123";

        SignatureController.SignRequest request = SignatureController.SignRequest.builder()
                .contractId(contractId)
                .kycSessionId(kycSessionId)
                .signerId(signerId)
                .certificateThumbprint(certThumbprint)
                .build();

        Signature mockSignature = Signature.builder().id(UUID.randomUUID()).build();

        when(signatureUseCase.signContract(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(mockSignature));

        // Act & Assert
        webTestClient
                .post()
                .uri("/api/v1/signatures/sign")
                .header("X-Forwarded-For", "10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        verify(signatureUseCase).signContract(
                eq(contractId),
                eq(kycSessionId),
                eq(signerId),
                eq(certThumbprint),
                eq("127.0.0.1"), // Ensure it didn't use the 10.0.0.1 spoofed header
                any()
        );
    }

    /**
     * Regression test: the real SignatureInteractor must surface a domain
     * ResourceNotFoundException when prepareContractHash's contract lookup
     * returns empty, so GlobalExceptionHandler maps it to 404 instead of
     * letting the empty Mono silently complete as a 200 with no body.
     */
    @Test
    void prepare_ShouldReturnNotFoundThroughRealInteractor_whenContractMissing() {
        // Arrange
        UUID contractId = UUID.randomUUID();

        ContractRepositoryPort contractRepositoryPort = mock(ContractRepositoryPort.class);
        when(contractRepositoryPort.findById(contractId)).thenReturn(Mono.empty());

        SignatureInteractor realInteractor = new SignatureInteractor(
                contractRepositoryPort,
                mock(SignatureServicePort.class),
                mock(SignatureRepositoryPort.class),
                mock(AuditTrailRepositoryPort.class),
                mock(EncryptionPort.class),
                mock(PdfTemplateCompiler.class),
                mock(StoragePort.class),
                mock(KycRepositoryPort.class)
        );

        // Real prepareContractHash() path: the interactor's switchIfEmpty is
        // what throws ResourceNotFoundException when the repository port
        // returns empty.
        Mono<String> notFoundMono = realInteractor.prepareContractHash(contractId);
        when(signatureUseCase.prepareContractHash(eq(contractId))).thenReturn(notFoundMono);

        // Act & Assert
        webTestClient
                .post()
                .uri("/api/v1/signatures/prepare?contractId=" + contractId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.errorCode").isEqualTo("NOT_FOUND");
    }
}
