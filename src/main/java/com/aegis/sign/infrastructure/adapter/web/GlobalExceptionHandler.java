package com.aegis.sign.infrastructure.adapter.web;

import com.aegis.sign.domain.exception.KycTechnicalException;
import com.aegis.sign.domain.exception.KycUserException;
import com.aegis.sign.domain.exception.PersistenceSerializationException;
import com.aegis.sign.domain.exception.ResourceNotFoundException;
import com.aegis.sign.domain.exception.TemplateNotFoundException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGeneralException(Exception ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage(), "INTERNAL_SERVER_ERROR")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBadRequest(IllegalArgumentException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "BAD_REQUEST")));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleNotFound(ResourceNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND")));
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleTemplateNotFound(TemplateNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "TEMPLATE_NOT_FOUND")));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalState(IllegalStateException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_STATE")));
    }

    @ExceptionHandler(KycTechnicalException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleKycTechnicalException(KycTechnicalException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode())));
    }

    @ExceptionHandler(KycUserException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleKycUserException(KycUserException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode())));
    }

    @ExceptionHandler(PersistenceSerializationException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handlePersistenceSerializationException(PersistenceSerializationException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage(), "PERSISTENCE_SERIALIZATION_ERROR")));
    }

    @ExceptionHandler(DataBufferLimitException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleUploadTooLarge(DataBufferLimitException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("Upload exceeds the maximum allowed size", "UPLOAD_TOO_LARGE")));
    }

    @ExceptionHandler(UnsupportedUploadTypeException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleUnsupportedUploadType(UnsupportedUploadTypeException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(ex.getMessage(), "UPLOAD_UNSUPPORTED_TYPE")));
    }
}
