package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.SignatureUseCase;
import com.aegis.sign.domain.model.Signature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureUseCase signatureUseCase;

    @PostMapping("/prepare")
    public Mono<ApiResponse<String>> prepare(@RequestParam UUID contractId) {
        return signatureUseCase.prepareContractHash(contractId)
                .map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<Signature>> getSignature(@PathVariable UUID id) {
        return signatureUseCase.getSignature(id)
                .map(ApiResponse::success);
    }

    @GetMapping
    public Mono<ApiResponse<Page<Signature>>> listByContractId(
            @RequestParam UUID contractId,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        return signatureUseCase.listByContractId(contractId, pageable)
                .map(ApiResponse::success);
    }

    @PostMapping("/sign")
    public Mono<ApiResponse<Signature>> sign(@RequestBody SignRequest request,
                                             @RequestHeader(value = "X-Forwarded-For", defaultValue = "127.0.0.1") String ipAddress,
                                             @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent) {
        return signatureUseCase.signContract(
                request.getContractId(),
                request.getKycSessionId(),
                request.getSignerId(),
                request.getCertificateThumbprint(),
                ipAddress,
                userAgent
        ).map(ApiResponse::success);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignRequest {
        private UUID contractId;
        private UUID kycSessionId;
        private String signerId;
        private String certificateThumbprint;
    }
}
