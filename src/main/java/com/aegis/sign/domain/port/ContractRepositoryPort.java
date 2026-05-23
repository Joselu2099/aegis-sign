package com.aegis.sign.domain.port;

import com.aegis.sign.domain.model.Contract;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ContractRepositoryPort {
    Mono<Contract> save(Contract contract);
    Mono<Contract> findById(UUID id);
}
