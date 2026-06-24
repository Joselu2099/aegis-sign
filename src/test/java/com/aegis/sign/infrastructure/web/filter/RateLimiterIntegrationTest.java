package com.aegis.sign.infrastructure.web.filter;

import com.aegis.sign.AbstractIntegrationTest;
import com.aegis.sign.application.ports.in.KycUseCase;
import com.aegis.sign.domain.model.KycSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "rate-limit.kyc.capacity=2",
    "rate-limit.kyc.refill-rate=0",
    "spring.cloud.vault.enabled=false",
    "spring.config.import=",
    "db.username=test",
    "db.password=test",
    "keystore.path=classpath:test-keystore.p12",
    "keystore.password=changeit",
    "keystore.alias=aegis-sign",
    "keystore.key-password=changeit",
    "minio.access-key=minioadmin",
    "minio.secret-key=minioadmin",
    "minio.bucket=aegis-sign",
    "minio.temp-bucket=aegis-sign-temp"
})
class RateLimiterIntegrationTest extends AbstractIntegrationTest {

    @MockBean
    private KycUseCase kycUseCase;

    @BeforeEach
    void setUp() {
        setupWebTestClient();
        
        when(kycUseCase.createSession(anyString())).thenReturn(Mono.just(
                KycSession.builder()
                        .id(UUID.randomUUID())
                        .status(KycSession.KycStatus.PENDING_DOCUMENTS)
                        .build()
        ));
    }

    @Test
    void whenRateLimitExceeded_thenReturnsTooManyRequests() {
        // KYC endpoint
        String kycUrl = "/api/v1/kyc/sessions?signerId=test-user";

        // First 2 requests should be OK (capacity is 2)
        IntStream.range(0, 2).forEach(i -> {
            webTestClient.post()
                    .uri(kycUrl)
                    .exchange()
                    .expectStatus().isOk();
        });

        // 3rd request should be rate limited
        try {
            Thread.sleep(1100); // Wait for refillRate (1 second + buffer)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        webTestClient.post()
                .uri(kycUrl)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void whenNotKycEndpoint_thenNotRateLimited() {
        // Non-KYC endpoint (assuming one exists or returns 404 but not 429)
        String otherUrl = "/api/v1/other";

        IntStream.range(0, 10).forEach(i -> {
            webTestClient.get()
                    .uri(otherUrl)
                    .exchange()
                    .expectStatus().isNotFound(); // Should be 404, not 429
        });
    }
}
