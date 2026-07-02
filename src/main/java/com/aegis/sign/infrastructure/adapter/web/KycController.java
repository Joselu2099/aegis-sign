package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.application.ports.in.KycUseCase;
import com.aegis.sign.domain.model.KycSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc/sessions")
@RequiredArgsConstructor
public class KycController {

    /**
     * Uploads are joined in memory for OCR/biometric processing, so they must
     * be bounded: an unlimited join lets a single oversized upload exhaust
     * the heap. Exceeding the limit surfaces as 413, a type outside the
     * allowlist as 415.
     */
    static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
    private static final Set<MediaType> DOCUMENT_TYPES =
            Set.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.APPLICATION_PDF);
    private static final Set<MediaType> BIOMETRIC_TYPES =
            Set.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);

    private final KycUseCase kycUseCase;

    @PostMapping
    public Mono<ApiResponse<KycSession>> createSession(@RequestParam String signerId) {
        return kycUseCase.createSession(signerId)
                .map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<KycSession>> getSession(@PathVariable UUID id) {
        return kycUseCase.getSession(id)
                .map(ApiResponse::success);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<KycSession>> submitIdDocument(
            @PathVariable UUID id,
            @RequestPart("file") Mono<FilePart> filePart) {

        return filePart.flatMap(fp -> readValidatedUpload(fp, DOCUMENT_TYPES))
                .flatMap(bytes -> kycUseCase.submitIdDocument(id, bytes))
                .map(ApiResponse::success);
    }

    @PostMapping(value = "/{id}/biometrics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<KycSession>> submitBiometrics(
            @PathVariable UUID id,
            @RequestPart("file") Mono<FilePart> filePart) {

        return filePart.flatMap(fp -> readValidatedUpload(fp, BIOMETRIC_TYPES))
                .flatMap(bytes -> kycUseCase.submitBiometrics(id, bytes))
                .map(ApiResponse::success);
    }

    private Mono<byte[]> readValidatedUpload(FilePart filePart, Set<MediaType> allowedTypes) {
        MediaType contentType = filePart.headers().getContentType();
        boolean allowed = contentType != null
                && allowedTypes.stream().anyMatch(type -> type.equalsTypeAndSubtype(contentType));
        if (!allowed) {
            return Mono.error(new UnsupportedUploadTypeException(contentType));
        }
        return DataBufferUtils.join(filePart.content(), MAX_UPLOAD_BYTES)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });
    }
}
