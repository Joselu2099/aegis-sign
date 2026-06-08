package com.aegis.sign.application.ports.in;

import com.aegis.sign.domain.model.Contract;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ContractUseCase {

    Mono<Contract> createContract(String templateId, List<String> signerIds, Map<String, Object> data);

    Mono<Contract> getContract(UUID id);
}
