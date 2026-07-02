package com.aegis.sign.infrastructure.adapter.web;

import org.springframework.http.MediaType;

/**
 * Web-layer rejection of an upload whose content type is outside the
 * endpoint's allowlist. Kept separate from ResponseStatusException because
 * the WebFlux ResponseEntityExceptionHandler base class already maps that
 * type (an extra handler would be ambiguous and break context startup).
 */
public class UnsupportedUploadTypeException extends RuntimeException {

    public UnsupportedUploadTypeException(MediaType contentType) {
        super("Unsupported upload content type: " + contentType);
    }
}
