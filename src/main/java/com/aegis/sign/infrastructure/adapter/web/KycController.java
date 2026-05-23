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
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycUseCase kycUseCase;

    @PostMapping("/session")
    public Mono<ApiResponse<KycSession>> createSession(@RequestParam String signerId) {
        return kycUseCase.createSession(signerId)
                .map(ApiResponse::success);
    }

    @GetMapping("/session/{id}")
    public Mono<ApiResponse<KycSession>> getSession(@PathVariable UUID id) {
        return kycUseCase.verifySession(id)
                .map(ApiResponse::success);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<KycSession>> upload(
            @RequestPart("file") Mono<FilePart> filePart,
            @RequestParam("type") String type,
            @RequestParam("sessionId") UUID sessionId) {
        
        return filePart.flatMap(fp -> DataBufferUtils.join(fp.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> kycUseCase.uploadDocument(sessionId, type, bytes))
                .map(ApiResponse::success));
    }
}
