package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.AuditTrail;
import com.aegis.sign.infrastructure.adapter.db.entity.AuditTrailEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.AuditTrailRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditTrailRepositoryAdapterTest {

    @Mock
    private AuditTrailRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private AuditTrailRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuditTrailRepositoryAdapter(repository, objectMapper);
    }

    /**
     * This is the test that would have caught Bug 2: the old toEntity()/toDomain()
     * implementation only serialized/deserialized the `events` list into
     * trail_manifest, silently dropping ocrMrzResults, biometricScore,
     * preSignatureHash and postSignatureHash. A full round trip through save()
     * and then back through toDomain() must preserve every field.
     */
    @Test
    void save_shouldPersistAllAuditTrailFieldsInTrailManifestJson_andRoundTripThemBackOnRead() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID kycSessionId = UUID.randomUUID();

        AuditTrail.AuditTrailEvent event = AuditTrail.AuditTrailEvent.builder()
                .eventType("SIGNATURE")
                .timestamp(LocalDateTime.of(2026, 6, 24, 10, 30))
                .description("Contract signed by signer-1")
                .ipAddress("10.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        AuditTrail auditTrail = AuditTrail.builder()
                .id(id)
                .contractId(contractId)
                .kycSessionId(kycSessionId)
                .ocrMrzResults("MRZ Valid: true, Message: null, Metadata: {docType=passport}")
                .biometricScore(0.987)
                .preSignatureHash("pre-hash-abc")
                .postSignatureHash("post-hash-xyz")
                .events(List.of(event))
                .build();

        ArgumentCaptor<AuditTrailEntity> entityCaptor = ArgumentCaptor.forClass(AuditTrailEntity.class);
        when(repository.save(entityCaptor.capture())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act: save
        StepVerifier.create(adapter.save(auditTrail))
                .expectNextCount(1)
                .verifyComplete();

        // Assert: the entity handed to the repository contains a trail_manifest
        // JSON payload with ALL domain fields, not just events.
        AuditTrailEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getTrailManifest()).isNotNull();
        String manifestJson = savedEntity.getTrailManifest().asString();

        assertThat(manifestJson).contains("\"ocrMrzResults\"");
        assertThat(manifestJson).contains("MRZ Valid: true");
        assertThat(manifestJson).contains("\"biometricScore\"");
        assertThat(manifestJson).contains("0.987");
        assertThat(manifestJson).contains("\"preSignatureHash\"");
        assertThat(manifestJson).contains("pre-hash-abc");
        assertThat(manifestJson).contains("\"postSignatureHash\"");
        assertThat(manifestJson).contains("post-hash-xyz");
        assertThat(manifestJson).contains("\"events\"");
        assertThat(manifestJson).contains("SIGNATURE");

        // Act: feed that same entity back through findById -> toDomain() and verify
        // every field survives the round trip.
        when(repository.findById(id)).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(adapter.findById(id))
                .assertNext(roundTripped -> {
                    assertThat(roundTripped.getId()).isEqualTo(id);
                    assertThat(roundTripped.getContractId()).isEqualTo(contractId);
                    assertThat(roundTripped.getKycSessionId()).isEqualTo(kycSessionId);
                    assertThat(roundTripped.getOcrMrzResults()).isEqualTo(auditTrail.getOcrMrzResults());
                    assertThat(roundTripped.getBiometricScore()).isEqualTo(auditTrail.getBiometricScore());
                    assertThat(roundTripped.getPreSignatureHash()).isEqualTo(auditTrail.getPreSignatureHash());
                    assertThat(roundTripped.getPostSignatureHash()).isEqualTo(auditTrail.getPostSignatureHash());
                    assertThat(roundTripped.getEvents()).hasSize(1);
                    assertThat(roundTripped.getEvents().get(0).getEventType()).isEqualTo("SIGNATURE");
                    assertThat(roundTripped.getEvents().get(0).getDescription()).isEqualTo("Contract signed by signer-1");
                    assertThat(roundTripped.getEvents().get(0).getIpAddress()).isEqualTo("10.0.0.1");
                    assertThat(roundTripped.getEvents().get(0).getUserAgent()).isEqualTo("Mozilla/5.0");
                })
                .verifyComplete();
    }

    @Test
    void updateFinalSignedPdfUri_shouldDelegateToRepositoryModifyingQuery() {
        // Arrange
        UUID id = UUID.randomUUID();
        String uri = "audit-trails/" + id + "-audit-trail.pdf";
        when(repository.updateFinalSignedPdfUri(eq(id), eq(uri))).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(adapter.updateFinalSignedPdfUri(id, uri))
                .verifyComplete();

        // Assert
        verify(repository).updateFinalSignedPdfUri(id, uri);
    }

    /**
     * Important fix verification: if serialization of the manifest fails,
     * save() must propagate the error rather than silently persisting an
     * empty/corrupted trail_manifest payload. Audit trail data is legal
     * evidence, so swallowing the failure and writing "{}" would be a
     * data-integrity bug.
     */
    @Test
    void save_shouldPropagateError_whenManifestSerializationFails() throws JsonProcessingException {
        // Arrange
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.databind.JsonMappingException(null, "boom"));
        AuditTrailRepositoryAdapter failingAdapter = new AuditTrailRepositoryAdapter(repository, failingMapper);

        AuditTrail auditTrail = AuditTrail.builder()
                .id(UUID.randomUUID())
                .contractId(UUID.randomUUID())
                .kycSessionId(UUID.randomUUID())
                .events(List.of())
                .build();

        // Act & Assert
        StepVerifier.create(failingAdapter.save(auditTrail))
                .expectErrorMatches(ex -> ex instanceof AuditTrailRepositoryAdapter.AuditTrailPersistenceException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();

        // The repository must never be called with a corrupted/empty manifest.
        verify(repository, never()).save(any());
    }

    /**
     * Important fix verification: if the persisted trail_manifest JSON is
     * malformed/unreadable, toDomain() must propagate the error rather than
     * silently returning an AuditTrail with empty events and null fields
     * that looks like a valid-but-empty record.
     */
    @Test
    void findById_shouldPropagateError_whenManifestDeserializationFails() {
        // Arrange
        UUID id = UUID.randomUUID();
        AuditTrailEntity corruptedEntity = AuditTrailEntity.builder()
                .id(id)
                .contractId(UUID.randomUUID())
                .kycSessionId(UUID.randomUUID())
                .trailManifest(Json.of("{not-valid-json"))
                .build();

        when(repository.findById(id)).thenReturn(Mono.just(corruptedEntity));

        // Act & Assert
        StepVerifier.create(adapter.findById(id))
                .expectErrorMatches(ex -> ex instanceof AuditTrailRepositoryAdapter.AuditTrailPersistenceException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();
    }

    @Test
    void findByContractId_shouldRestoreAllFieldsFromManifest() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID kycSessionId = UUID.randomUUID();

        AuditTrail auditTrail = AuditTrail.builder()
                .id(id)
                .contractId(contractId)
                .kycSessionId(kycSessionId)
                .ocrMrzResults("MRZ Valid: false")
                .biometricScore(0.42)
                .preSignatureHash("pre")
                .postSignatureHash("post")
                .events(List.of())
                .build();

        ArgumentCaptor<AuditTrailEntity> captor = ArgumentCaptor.forClass(AuditTrailEntity.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        adapter.save(auditTrail).block();

        when(repository.findByContractId(contractId)).thenReturn(Mono.just(captor.getValue()));

        // Act & Assert
        StepVerifier.create(adapter.findByContractId(contractId))
                .assertNext(result -> {
                    assertThat(result.getBiometricScore()).isEqualTo(0.42);
                    assertThat(result.getOcrMrzResults()).isEqualTo("MRZ Valid: false");
                    assertThat(result.getPreSignatureHash()).isEqualTo("pre");
                    assertThat(result.getPostSignatureHash()).isEqualTo("post");
                })
                .verifyComplete();
    }
}
