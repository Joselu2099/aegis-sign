package com.aegis.sign.infrastructure.adapter.db.repository;

import com.aegis.sign.infrastructure.adapter.db.entity.ContractEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractRepository extends ReactiveCrudRepository<ContractEntity, UUID> {
}
