package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.exception.PersistenceSerializationException;
import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.ContractEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.ContractRepository;
import io.r2dbc.postgresql.codec.Json;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContractRepositoryAdapter implements ContractRepositoryPort {

    private final ContractRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Contract> save(Contract contract) {
        return toEntity(contract)
                .flatMap(repository::save)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Contract> findById(UUID id) {
        return repository.findById(id)
                .flatMap(this::toDomain);
    }

    private Mono<ContractEntity> toEntity(Contract contract) {
        return Mono.fromCallable(() -> ContractEntity.builder()
                .id(contract.getId())
                .templateId(contract.getTemplateId())
                .status(contract.getStatus().name())
                .documentHashSha256(contract.getContentHash())
                .minioUri(contract.getUri())
                .signerIds(Json.of(serializeSignerIds(contract.getSignerIds(), contract.getId())))
                .build());
    }

    private Mono<Contract> toDomain(ContractEntity entity) {
        return Mono.fromCallable(() -> Contract.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .status(Contract.ContractStatus.valueOf(entity.getStatus()))
                .contentHash(entity.getDocumentHashSha256())
                .uri(entity.getMinioUri())
                .signerIds(deserializeSignerIds(entity.getSignerIds() != null ? entity.getSignerIds().asString() : null, entity.getId()))
                .build());
    }

    private String serializeSignerIds(List<String> signerIds, UUID contractId) {
        if (signerIds == null || signerIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(signerIds);
        } catch (JsonProcessingException e) {
            throw new PersistenceSerializationException("Failed to serialize signerIds for contract " + contractId, e);
        }
    }

    private List<String> deserializeSignerIds(String json, UUID contractId) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new PersistenceSerializationException("Failed to deserialize signerIds for contract " + contractId, e);
        }
    }
}
