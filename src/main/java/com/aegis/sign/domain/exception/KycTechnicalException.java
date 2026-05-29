package com.aegis.sign.domain.exception;

public class KycTechnicalException extends RuntimeException {
    private final String errorCode;

    public KycTechnicalException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public KycTechnicalException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
