package com.aegis.sign.infrastructure.adapter.web;

<<<<<<< HEAD
import com.aegis.sign.domain.exception.KycTechnicalException;
import com.aegis.sign.domain.exception.KycUserException;
=======
import com.aegis.sign.domain.exception.TemplateNotFoundException;
>>>>>>> sprint-2-tasks-01-09-domain-api
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

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
    public Mono<ProblemDetail> handleTemplateNotFound(TemplateNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Template Not Found");
        problemDetail.setType(URI.create("https://api.aegis-sign.com/errors/template-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return Mono.just(problemDetail);
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
}
