package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.KycUseCase;
import com.aegis.sign.domain.model.KycSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc/sessions")
@RequiredArgsConstructor
public class KycController {

    private final KycUseCase kycUseCase;

    @PostMapping
    public Mono<ApiResponse<KycSession>> createSession(@RequestParam String signerId) {
        return kycUseCase.createSession(signerId)
                .map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<KycSession>> getSession(@PathVariable UUID id) {
        return kycUseCase.verifySession(id)
                .map(ApiResponse::success);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<KycSession>> submitIdDocument(
            @PathVariable UUID id,
            @RequestPart("file") Mono<FilePart> filePart) {
        
        return filePart.flatMap(fp -> DataBufferUtils.join(fp.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> kycUseCase.submitIdDocument(id, bytes))
                .map(ApiResponse::success));
    }

    @PostMapping(value = "/{id}/biometrics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<KycSession>> submitBiometrics(
            @PathVariable UUID id,
            @RequestPart("file") Mono<FilePart> filePart) {
        
        return filePart.flatMap(fp -> DataBufferUtils.join(fp.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> kycUseCase.submitBiometrics(id, bytes))
                .map(ApiResponse::success));
    }
}
