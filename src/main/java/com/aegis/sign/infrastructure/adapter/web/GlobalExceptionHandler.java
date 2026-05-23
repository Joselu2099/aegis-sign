package com.aegis.sign.infrastructure.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ProblemDetail> handleGeneralException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.aegis-sign.com/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.aegis-sign.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.aegis-sign.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return Mono.just(problemDetail);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Invalid State");
        problemDetail.setType(URI.create("https://api.aegis-sign.com/errors/invalid-state"));
        problemDetail.setProperty("timestamp", Instant.now());
        return Mono.just(problemDetail);
    }
}
