package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.KycUseCase;
import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.infrastructure.adapter.web.filter.TokenBucketRateLimiterFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = KycController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TokenBucketRateLimiterFilter.class
        )
)
class KycControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private KycUseCase kycUseCase;

    @Test
    void createSession_ShouldReturn200() {
        String signerId = "signer123";
        KycSession session = KycSession.builder()
                .id(UUID.randomUUID())
                .signerId(signerId)
                .status(KycSession.KycStatus.PENDING_DOCUMENTS)
                .build();

        when(kycUseCase.createSession(signerId)).thenReturn(Mono.just(session));

        webTestClient.post()
                .uri("/api/v1/kyc/sessions?signerId={signerId}", signerId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(session.getId().toString())
                .jsonPath("$.data.signerId").isEqualTo(signerId);
    }

    @Test
    void getSession_ShouldReturn200() {
        UUID id = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(id)
                .status(KycSession.KycStatus.PENDING_DOCUMENTS)
                .build();

        when(kycUseCase.getSession(id)).thenReturn(Mono.just(session));

        webTestClient.get()
                .uri("/api/v1/kyc/sessions/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(id.toString());
    }

    @Test
    void submitIdDocument_ShouldReturn200() {
        UUID id = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(id)
                .status(KycSession.KycStatus.PROCESSING)
                .build();

        when(kycUseCase.submitIdDocument(eq(id), any(byte[].class))).thenReturn(Mono.just(session));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "dummy content".getBytes())
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "form-data; name=file; filename=doc.pdf");

        webTestClient.post()
                .uri("/api/v1/kyc/sessions/{id}/documents", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(id.toString());
    }

    @Test
    void submitBiometrics_ShouldReturn200() {
        UUID id = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(id)
                .status(KycSession.KycStatus.PROCESSING)
                .build();

        when(kycUseCase.submitBiometrics(eq(id), any(byte[].class))).thenReturn(Mono.just(session));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "dummy biometrics".getBytes())
                .contentType(MediaType.IMAGE_JPEG)
                .header("Content-Disposition", "form-data; name=file; filename=bio.jpg");

        webTestClient.post()
                .uri("/api/v1/kyc/sessions/{id}/biometrics", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(id.toString());
    }

    @Test
    void submitIdDocument_ShouldReturn415_WhenContentTypeIsNotAllowed() {
        UUID id = UUID.randomUUID();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "malicious".getBytes())
                .contentType(MediaType.TEXT_PLAIN)
                .header("Content-Disposition", "form-data; name=file; filename=doc.txt");

        webTestClient.post()
                .uri("/api/v1/kyc/sessions/{id}/documents", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void submitBiometrics_ShouldReturn415_WhenPdfIsUploadedAsSelfie() {
        UUID id = UUID.randomUUID();

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "not a selfie".getBytes())
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "form-data; name=file; filename=bio.pdf");

        webTestClient.post()
                .uri("/api/v1/kyc/sessions/{id}/biometrics", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isEqualTo(415);
    }

    @Test
    void submitIdDocument_ShouldReturn413_WhenUploadExceedsMaxSize() {
        UUID id = UUID.randomUUID();
        byte[] oversized = new byte[KycController.MAX_UPLOAD_BYTES + 1];

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", oversized)
                .contentType(MediaType.IMAGE_JPEG)
                .header("Content-Disposition", "form-data; name=file; filename=doc.jpg");

        webTestClient.post()
                .uri("/api/v1/kyc/sessions/{id}/documents", id)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isEqualTo(413)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("UPLOAD_TOO_LARGE");
    }
}
