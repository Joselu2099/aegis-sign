package com.aegis.sign.domain.exception;

/**
 * Thrown when a JSON column payload (e.g. an audit trail manifest, a list of
 * signer ids, or KYC extracted data) cannot be serialized or deserialized.
 * These payloads carry data-integrity- and evidentiary-sensitive information,
 * so a (de)serialization failure must fail the calling reactive chain rather
 * than silently persisting or returning a corrupted/incomplete record.
 */
public class PersistenceSerializationException extends RuntimeException {

    public PersistenceSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
