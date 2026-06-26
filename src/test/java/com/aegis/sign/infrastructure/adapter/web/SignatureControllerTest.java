package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.SignatureUseCase;
import com.aegis.sign.domain.model.Signature;
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
}
