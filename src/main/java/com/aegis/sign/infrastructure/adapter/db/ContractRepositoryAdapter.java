package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.Contract;
import com.aegis.sign.domain.port.ContractRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.ContractEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContractRepositoryAdapter implements ContractRepositoryPort {

    private final ContractRepository repository;

    @Override
    public Mono<Contract> save(Contract contract) {
        return repository.save(toEntity(contract))
                .map(this::toDomain);
    }

    @Override
    public Mono<Contract> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    private ContractEntity toEntity(Contract contract) {
        return ContractEntity.builder()
                .id(contract.getId())
                .templateId(contract.getTemplateId())
                .status(contract.getStatus().name())
                .documentHashSha256(contract.getContentHash())
                .minioUri(contract.getUri())
                .build();
    }

    private Contract toDomain(ContractEntity entity) {
        return Contract.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .status(Contract.ContractStatus.valueOf(entity.getStatus()))
                .contentHash(entity.getDocumentHashSha256())
                .uri(entity.getMinioUri())
                .build();
    }
}
