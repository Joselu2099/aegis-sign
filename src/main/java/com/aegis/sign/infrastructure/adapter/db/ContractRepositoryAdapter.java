package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.domain.exception.ResourceNotFoundException;
import com.aegis.sign.infrastructure.adapter.db.entity.ContractEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.ContractRepository;
import io.r2dbc.postgresql.codec.Json;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractRepositoryAdapter implements ContractRepositoryPort {

    private static final String EMPTY_JSON_ARRAY = "[]";

    private final ContractRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Contract> save(Contract contract) {
        return repository.save(toEntity(contract))
                .map(this::toDomain);
    }

    @Override
    public Mono<Contract> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Contract not found: " + id)));
    }

    private ContractEntity toEntity(Contract contract) {
        return ContractEntity.builder()
                .id(contract.getId())
                .templateId(contract.getTemplateId())
                .status(contract.getStatus().name())
                .documentHashSha256(contract.getContentHash())
                .minioUri(contract.getUri())
                .signerIds(Json.of(serializeSignerIds(contract.getSignerIds())))
                .build();
    }

    private Contract toDomain(ContractEntity entity) {
        return Contract.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .status(Contract.ContractStatus.valueOf(entity.getStatus()))
                .contentHash(entity.getDocumentHashSha256())
                .uri(entity.getMinioUri())
                .signerIds(deserializeSignerIds(entity.getSignerIds() != null ? entity.getSignerIds().asString() : null))
                .build();
    }

    private String serializeSignerIds(List<String> signerIds) {
        if (signerIds == null || signerIds.isEmpty()) {
            return EMPTY_JSON_ARRAY;
        }
        try {
            return objectMapper.writeValueAsString(signerIds);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize signerIds", e);
            return EMPTY_JSON_ARRAY;
        }
    }

    private List<String> deserializeSignerIds(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize signerIds: {}", json, e);
            return Collections.emptyList();
        }
    }
}
