package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.ContractUseCase;
import com.aegis.sign.domain.model.Contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractUseCase contractUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<Contract>> createContract(@RequestBody CreateContractRequest request) {
        return contractUseCase.createContract(request.getTemplateId(), request.getSignerIds(), request.getData())
                .map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<Contract>> getContract(@PathVariable UUID id) {
        return contractUseCase.getContract(id)
                .map(ApiResponse::success);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateContractRequest {
        private String templateId;
        private List<String> signerIds;
        private Map<String, Object> data;
    }
}

