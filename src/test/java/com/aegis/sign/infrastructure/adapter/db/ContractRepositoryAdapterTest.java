package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.exception.PersistenceSerializationException;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.infrastructure.adapter.db.entity.ContractEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.ContractRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractRepositoryAdapterTest {

    @Mock
    private ContractRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ContractRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ContractRepositoryAdapter(repository, objectMapper);
    }

    @Test
    void save_shouldPersistSignerIdsAndRoundTripThemBackOnRead() {
        UUID id = UUID.randomUUID();
        Contract contract = Contract.builder()
                .id(id)
                .templateId("template-1")
                .status(Contract.ContractStatus.DRAFT)
                .contentHash("hash")
                .uri("uri")
                .signerIds(List.of("signer-1", "signer-2"))
                .build();

        ArgumentCaptor<ContractEntity> captor = ArgumentCaptor.forClass(ContractEntity.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(adapter.save(contract))
                .assertNext(saved -> assertThat(saved.getSignerIds()).containsExactly("signer-1", "signer-2"))
                .verifyComplete();

        assertThat(captor.getValue().getSignerIds().asString()).contains("signer-1", "signer-2");
    }

    /**
     * Important fix verification: if serialization of signerIds fails,
     * save() must propagate the error rather than silently persisting "[]"
     * and losing the real signer list.
     */
    @Test
    void save_shouldPropagateError_whenSignerIdsSerializationFails() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonMappingException(null, "boom"));
        ContractRepositoryAdapter failingAdapter = new ContractRepositoryAdapter(repository, failingMapper);

        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .templateId("template-1")
                .status(Contract.ContractStatus.DRAFT)
                .contentHash("hash")
                .uri("uri")
                .signerIds(List.of("signer-1"))
                .build();

        StepVerifier.create(failingAdapter.save(contract))
                .expectErrorMatches(ex -> ex instanceof PersistenceSerializationException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();

        verify(repository, never()).save(any());
    }

    /**
     * Important fix verification: if the persisted signer_ids JSON is
     * malformed/unreadable, findById() must propagate the error rather than
     * silently returning a Contract with an empty signer list that looks
     * like a valid contract with no signers.
     */
    @Test
    void findById_shouldPropagateError_whenSignerIdsDeserializationFails() {
        UUID id = UUID.randomUUID();
        ContractEntity corruptedEntity = ContractEntity.builder()
                .id(id)
                .templateId("template-1")
                .status(Contract.ContractStatus.DRAFT.name())
                .documentHashSha256("hash")
                .minioUri("uri")
                .signerIds(Json.of("{not-valid-json"))
                .build();

        when(repository.findById(id)).thenReturn(Mono.just(corruptedEntity));

        StepVerifier.create(adapter.findById(id))
                .expectErrorMatches(ex -> ex instanceof PersistenceSerializationException
                        && ex.getCause() instanceof JsonProcessingException)
                .verify();
    }
}
