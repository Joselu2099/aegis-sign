package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.SignatureUseCase;
import com.aegis.sign.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Covers the GET /api/v1/signatures/audit-trail/{contractId} endpoint that
 * exposes SignatureUseCase#generateAndSignAuditTrailPdf as raw PDF bytes.
 */
@WebFluxTest(
        controllers = SignatureController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {com.aegis.sign.infrastructure.web.filter.TokenBucketRateLimiterFilter.class}
        )
)
class SignatureControllerAuditTrailTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SignatureUseCase signatureUseCase;

    @Test
    void getAuditTrailPdf_shouldReturnPdfBytesWithPdfContentType() {
        // Arrange
        UUID contractId = UUID.randomUUID();
        byte[] signedPdfBytes = "%PDF-1.4 fake-signed-audit-trail".getBytes();

        when(signatureUseCase.generateAndSignAuditTrailPdf(eq(contractId)))
                .thenReturn(Mono.just(signedPdfBytes));

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/signatures/audit-trail/{contractId}", contractId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectBody(byte[].class)
                .isEqualTo(signedPdfBytes);
    }

    @Test
    void getAuditTrailPdf_shouldReturnErrorWhenAuditTrailNotFound() {
        // Arrange
        UUID contractId = UUID.randomUUID();
        when(signatureUseCase.generateAndSignAuditTrailPdf(eq(contractId)))
                .thenReturn(Mono.error(new ResourceNotFoundException("Audit trail not found for contract: " + contractId)));

        // Act & Assert: GlobalExceptionHandler maps ResourceNotFoundException to 404.
        webTestClient.get()
                .uri("/api/v1/signatures/audit-trail/{contractId}", contractId)
                .exchange()
                .expectStatus().isNotFound();
    }
}
