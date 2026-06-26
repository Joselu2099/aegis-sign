package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.exception.PersistenceSerializationException;
import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.infrastructure.adapter.db.entity.KycSessionEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.KycSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycRepositoryAdapterTest {

    @Mock
    private KycSessionRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private KycRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KycRepositoryAdapter(repository, objectMapper);
    }

    @Test
    void save_shouldPersistExtractedDataAndRoundTripItBackOnRead() {
        UUID id = UUID.randomUUID();
        KycSession session = KycSession.builder()
                .id(id)
                .status(KycSession.KycStatus.APPROVED)
                .documentMetadata(Map.of("docType", "passport"))
                .faceMatchScore(0.99)
                .signerId("signer-1")
                .mrzValid(true)
                .biometricValid(true)
                .build();

        ArgumentCaptor<KycSessionEntity> captor = ArgumentCaptor.forClass(KycSessionEntity.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(adapter.save(session))
                .assertNext(saved -> {
                    assertThat(saved.getSignerId()).isEqualTo("signer-1");
                    assertThat(saved.isMrzValid()).isTrue();
                    assertThat(saved.isBiometricValid()).isTrue();
                    assertThat(saved.getDocumentMetadata()).containsEntry("docType", "passport");
                })
                .verifyComplete();

        assertThat(captor.getValue().getExtractedData().asString()).contains("signer-1", "passport");
    }

    /**
     * Important fix verification: if serialization of the KYC extracted
     * data fails, save() must propagate the error rather than silently
     * persisting extractedData = null and losing identity-verification
     * evidence (MRZ/biometric results, signer id, document metadata).
     */
    @Test
    void save_shouldPropagateError_whenExtractedDataSerializationFails() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonMappingException(null, "boom"));
        KycRepositoryAdapter failingAdapter = new KycRepositoryAdapter(repository, failingMapper);

        KycSession session = KycSession.builder()
                .id(UUID.randomUUID())
                .status(KycSession.KycStatus.APPROVED)
                .documentMetadata(Map.of("docType", "passport"))
                .signerId("signer-1")
                .build();

        StepVerifier.create(failingAdapter.save(session))
                .expectErrorMatches(ex -> ex instanceof PersistenceSerializationException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();

        verify(repository, never()).save(any());
    }

    /**
     * Important fix verification: if the persisted extracted_data JSON is
     * malformed/unreadable, findById() must propagate the error rather than
     * silently returning a KycSession with empty/default metadata, mrzValid
     * = false, biometricValid = false, and a missing signerId — which would
     * look like a valid-but-failed verification rather than corrupted data.
     */
    @Test
    void findById_shouldPropagateError_whenExtractedDataDeserializationFails() {
        UUID id = UUID.randomUUID();
        KycSessionEntity corruptedEntity = KycSessionEntity.builder()
                .id(id)
                .status(KycSession.KycStatus.APPROVED.name())
                .extractedData(Json.of("{not-valid-json"))
                .biometricScore(0.99)
                .build();

        when(repository.findById(id)).thenReturn(Mono.just(corruptedEntity));

        StepVerifier.create(adapter.findById(id))
                .expectErrorMatches(ex -> ex instanceof PersistenceSerializationException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();
    }
}
