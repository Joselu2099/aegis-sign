package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.exception.PersistenceSerializationException;
import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.infrastructure.adapter.db.entity.SignatureEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.SignatureRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureRepositoryAdapterTest {

    @Mock
    private SignatureRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private SignatureRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SignatureRepositoryAdapter(repository, objectMapper);
    }

    @Test
    void save_shouldPersistSignerInfoAndRoundTripItBackOnRead() {
        UUID id = UUID.randomUUID();
        Signature signature = Signature.builder()
                .id(id)
                .contractId(UUID.randomUUID())
                .signerId("signer-1")
                .certificateThumbprint("thumbprint")
                .timestamp(LocalDateTime.now())
                .build();

        ArgumentCaptor<SignatureEntity> captor = ArgumentCaptor.forClass(SignatureEntity.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(adapter.save(signature))
                .assertNext(saved -> assertThat(saved.getSignerId()).isEqualTo("signer-1"))
                .verifyComplete();

        assertThat(captor.getValue().getSignerInfo().asString()).contains("signer-1");
    }

    /**
     * Critical fix verification: a signerId containing double quotes must
     * survive JSON serialization and be correctly deserialized on read.
     * With the old string-concatenation approach, inner quotes would break
     * the JSON structure, and the regex-based extractSignerId would return
     * a truncated value.
     */
    @Test
    void save_shouldHandleSignerIdWithSpecialCharacters() {
        UUID id = UUID.randomUUID();
        Signature signature = Signature.builder()
                .id(id)
                .contractId(UUID.randomUUID())
                .signerId("signer\"id\"with\"quotes")
                .certificateThumbprint("thumbprint")
                .timestamp(LocalDateTime.now())
                .build();

        ArgumentCaptor<SignatureEntity> captor = ArgumentCaptor.forClass(SignatureEntity.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(adapter.save(signature))
                .assertNext(saved -> assertThat(saved.getSignerId()).isEqualTo("signer\"id\"with\"quotes"))
                .verifyComplete();

        assertThat(captor.getValue().getSignerInfo().asString())
                .doesNotContain("signer\"id\"with\"quotes") // raw quotes would be escaped
                .contains("signer\\\"id\\\"with\\\"quotes");
    }

    /**
     * Important fix verification: if serialization of signerInfo fails,
     * save() must propagate the error rather than silently persisting
     * malformed JSON and losing the signer identity.
     */
    @Test
    void save_shouldPropagateError_whenSignerInfoSerializationFails() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonMappingException(null, "boom"));
        SignatureRepositoryAdapter failingAdapter = new SignatureRepositoryAdapter(repository, failingMapper);

        Signature signature = Signature.builder()
                .id(UUID.randomUUID())
                .contractId(UUID.randomUUID())
                .signerId("signer-1")
                .certificateThumbprint("thumbprint")
                .timestamp(LocalDateTime.now())
                .build();

        StepVerifier.create(failingAdapter.save(signature))
                .expectErrorMatches(ex -> ex instanceof PersistenceSerializationException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();

        verify(repository, never()).save(any());
    }

    /**
     * Important fix verification: if the persisted signer_info JSON is
     * malformed/unreadable, findById() must propagate the error rather
     * than silently returning a Signature with a null/missing signerId
     * that looks like a valid signature with no associated signer.
     */
    @Test
    void findById_shouldPropagateError_whenSignerInfoDeserializationFails() {
        UUID id = UUID.randomUUID();
        SignatureEntity corruptedEntity = SignatureEntity.builder()
                .id(id)
                .contractId(UUID.randomUUID())
                .signerInfo(Json.of("{not-valid-json"))
                .x509CertificateSn("thumbprint")
                .build();

        when(repository.findById(id)).thenReturn(Mono.just(corruptedEntity));

        StepVerifier.create(adapter.findById(id))
                .expectErrorMatches(ex -> ex instanceof PersistenceSerializationException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();
    }
}
