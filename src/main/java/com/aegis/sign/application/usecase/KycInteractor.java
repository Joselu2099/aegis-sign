package com.aegis.sign.application.usecase;

import com.aegis.sign.application.ports.in.KycUseCase;
import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import com.aegis.sign.domain.port.StoragePort;
import com.aegis.sign.domain.service.MrzValidationService;
import com.aegis.sign.domain.service.OcrExtractorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycInteractor implements KycUseCase {

    private final KycRepositoryPort kycRepositoryPort;
    private final StoragePort storagePort;
    private final OcrExtractorService ocrExtractorService;
    private final MrzValidationService mrzValidationService;

    @Override
    public Mono<KycSession> createSession(String signerId) {
        KycSession session = KycSession.builder()
                .id(UUID.randomUUID())
                .signerId(signerId)
                .status(KycSession.KycStatus.PENDING)
                .documentMetadata(new java.util.HashMap<>())
                .mrzValid(false) // Initialize MRZ validity
                .build();
        return kycRepositoryPort.save(session);
    }

    @Override
    public Mono<KycSession> verifySession(UUID sessionId) {
        return kycRepositoryPort.findById(sessionId)
                .flatMap(session -> {
                    // Logic for session verification lifecycle
                    // For now, we simulate approval if session exists
                    session.setStatus(KycSession.KycStatus.APPROVED);
                    return kycRepositoryPort.save(session);
                });
    }

    @Override
    public Mono<KycSession> submitIdDocument(UUID sessionId, byte[] content) {
        return kycRepositoryPort.findById(sessionId)
                .flatMap(session -> {
                    // 1. Perform OCR extraction
                    Map<String, String> ocrData = ocrExtractorService.extractData(content);
                    session.setDocumentMetadata(ocrData); // Store all OCR data

                    // 2. Perform MRZ validation
                    boolean allMrzValid = performMrzValidation(session, ocrData);
                    session.setMrzValid(allMrzValid);
                    if (!allMrzValid) {
                        if (session.getMrzValidationErrorMessage() == null) {
                            session.setMrzValidationErrorMessage("MRZ validation failed due to an unknown error.");
                        }
                        session.setStatus(KycSession.KycStatus.MRZ_FAILED); // Set status to MRZ_FAILED
                    }

                    // Specific logic for ID document ingestion
                    session.getDocumentMetadata().put("ID_DOCUMENT", "UPLOADED");
                    return kycRepositoryPort.save(session);
                });
    }

    @Override
    public Mono<KycSession> submitBiometrics(UUID sessionId, byte[] content) {
        return kycRepositoryPort.findById(sessionId)
                .flatMap(session -> {
                    String biometricFilePath = "biometrics/" + sessionId.toString() + "/" + UUID.randomUUID().toString();
                    return storagePort.uploadTempFile(content, biometricFilePath)
                            .flatMap(path -> {
                                session.getDocumentMetadata().put("BIOMETRICS", path);
                                return kycRepositoryPort.save(session);
                            });
                });
    }

    private boolean performMrzValidation(KycSession session, Map<String, String> ocrData) {
        String mrzType = ocrData.get("mrzType");
        if (mrzType == null || "NONE".equals(mrzType)) {
            session.setMrzValidationErrorMessage("No MRZ found in the document.");
            return false;
        }

        boolean isValid = true;
        StringBuilder errorMessages = new StringBuilder();

        switch (mrzType) {
            case "TD3":
                isValid = validateTD3Mrz(ocrData, errorMessages);
                break;
            case "TD1":
                isValid = validateTD1Mrz(ocrData, errorMessages);
                break;
            case "TD2":
                isValid = validateTD2Mrz(ocrData, errorMessages);
                break;
            default:
                session.setMrzValidationErrorMessage("Unsupported MRZ type: " + mrzType);
                return false;
        }

        if (!isValid) {
            session.setMrzValidationErrorMessage(errorMessages.toString());
        }
        return isValid;
    }

    private boolean validateTD3Mrz(Map<String, String> ocrData, StringBuilder errorMessages) {
        boolean isValid = true;

        // Document Number Check
        String docNum = ocrData.get("documentNumber");
        String docNumCheckDigit = ocrData.get("documentNumberCheckDigit");
        if (docNum != null && docNumCheckDigit != null && docNumCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(docNum, docNumCheckDigit.charAt(0))) {
                errorMessages.append("Document Number Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Document Number or its check digit for TD3 validation. ");
            isValid = false;
        }

        // Birth Date Check
        String birthDate = ocrData.get("birthDate");
        String birthDateCheckDigit = ocrData.get("birthDateCheckDigit");
        if (birthDate != null && birthDateCheckDigit != null && birthDateCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(birthDate, birthDateCheckDigit.charAt(0))) {
                errorMessages.append("Birth Date Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Birth Date or its check digit for TD3 validation. ");
            isValid = false;
        }

        // Expiry Date Check
        String expiryDate = ocrData.get("expiryDate");
        String expiryDateCheckDigit = ocrData.get("expiryDateCheckDigit");
        if (expiryDate != null && expiryDateCheckDigit != null && expiryDateCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(expiryDate, expiryDateCheckDigit.charAt(0))) {
                errorMessages.append("Expiry Date Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Expiry Date or its check digit for TD3 validation. ");
            isValid = false;
        }

        // Personal Number Check (optional, but if present, validate)
        String personalNum = ocrData.get("personalNumber");
        String personalNumCheckDigit = ocrData.get("personalNumberCheckDigit");
        if (personalNum != null && personalNumCheckDigit != null && personalNumCheckDigit.length() == 1) {
            if (!personalNum.isBlank() && !personalNum.contains("<")) { // Only validate if not just fillers
                if (!mrzValidationService.validateChecksum(personalNum, personalNumCheckDigit.charAt(0))) {
                    errorMessages.append("Personal Number Check Digit invalid. ");
                    isValid = false;
                }
            }
        }

        // Composite Checksum
        String mrzLine1 = ocrData.get("mrzLine1");
        String mrzLine2 = ocrData.get("mrzLine2");
        String compositeCheckDigit = ocrData.get("compositeCheckDigit");

        if (mrzLine1 != null && mrzLine2 != null && compositeCheckDigit != null && compositeCheckDigit.length() == 1) {
            // TD3 Composite Checksum calculation combines parts of line 1 and line 2
            // Example structure:
            // P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<
            // L898902C<3UTO8001015F2405236<<<<<<<<<<<<<<0
            // The composite string is a concatenation of the Document Number, its check digit,
            // Nationality, Date of Birth, its check digit, Sex, Date of Expiry, its check digit,
            // Personal Number, its check digit, from line 2.
            // And then parts of line 1 (surname and given names).
            // However, the standard MRZ composite check digit usually covers only the machine-readable part,
            // which is typically composed from specific fields of line 2 for TD3.
            // Simplified for now: Assume composite check applies to a segment of line 2.
            // A common composite structure for TD3 (passport) is:
            // (Document Number + check) + (DOB + check) + (Expiry + check) + (Personal Number + check)
            // L898902C3 + 8001015 + 2405236 + <<<<<<<<<<<<<<0
            String compositeString = mrzLine2.substring(0, 10) + // Document Number + Check
                                     mrzLine2.substring(13, 20) + // Birth Date + Check
                                     mrzLine2.substring(21, 28) + // Expiry Date + Check
                                     mrzLine2.substring(28, 43);  // Personal Number + Check

            if (!mrzValidationService.validateChecksum(compositeString, compositeCheckDigit.charAt(0))) {
                errorMessages.append("Composite Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing data for Composite Check Digit validation for TD3. ");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateTD1Mrz(Map<String, String> ocrData, StringBuilder errorMessages) {
        boolean isValid = true;

        // Document Number Check
        String docNum = ocrData.get("documentNumber");
        String docNumCheckDigit = ocrData.get("documentNumberCheckDigit");
        if (docNum != null && docNumCheckDigit != null && docNumCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(docNum, docNumCheckDigit.charAt(0))) {
                errorMessages.append("Document Number Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Document Number or its check digit for TD1 validation. ");
            isValid = false;
        }

        // Birth Date Check
        String birthDate = ocrData.get("birthDate");
        String birthDateCheckDigit = ocrData.get("birthDateCheckDigit");
        if (birthDate != null && birthDateCheckDigit != null && birthDateCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(birthDate, birthDateCheckDigit.charAt(0))) {
                errorMessages.append("Birth Date Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Birth Date or its check digit for TD1 validation. ");
            isValid = false;
        }

        // Expiry Date Check
        String expiryDate = ocrData.get("expiryDate");
        String expiryDateCheckDigit = ocrData.get("expiryDateCheckDigit");
        if (expiryDate != null && expiryDateCheckDigit != null && expiryDateCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(expiryDate, expiryDateCheckDigit.charAt(0))) {
                errorMessages.append("Expiry Date Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Expiry Date or its check digit for TD1 validation. ");
            isValid = false;
        }

        // Composite Checksum
        // For TD1, the composite check digit covers the entire first and second MRZ lines.
        String mrzLine1 = ocrData.get("mrzLine1");
        String mrzLine2 = ocrData.get("mrzLine2");
        String compositeCheckDigit = ocrData.get("compositeCheckDigit");

        if (mrzLine1 != null && mrzLine2 != null && compositeCheckDigit != null && compositeCheckDigit.length() == 1) {
            String compositeString = mrzLine1.substring(0, 30) + mrzLine2.substring(0, 30);
            if (!mrzValidationService.validateChecksum(compositeString, compositeCheckDigit.charAt(0))) {
                errorMessages.append("Composite Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing data for Composite Check Digit validation for TD1. ");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateTD2Mrz(Map<String, String> ocrData, StringBuilder errorMessages) {
        boolean isValid = true;

        // Document Number Check
        String docNum = ocrData.get("documentNumber");
        String docNumCheckDigit = ocrData.get("documentNumberCheckDigit");
        if (docNum != null && docNumCheckDigit != null && docNumCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(docNum, docNumCheckDigit.charAt(0))) {
                errorMessages.append("Document Number Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Document Number or its check digit for TD2 validation. ");
            isValid = false;
        }

        // Birth Date Check
        String birthDate = ocrData.get("birthDate");
        String birthDateCheckDigit = ocrData.get("birthDateCheckDigit");
        if (birthDate != null && birthDateCheckDigit != null && birthDateCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(birthDate, birthDateCheckDigit.charAt(0))) {
                errorMessages.append("Birth Date Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Birth Date or its check digit for TD2 validation. ");
            isValid = false;
        }

        // Expiry Date Check
        String expiryDate = ocrData.get("expiryDate");
        String expiryDateCheckDigit = ocrData.get("expiryDateCheckDigit");
        if (expiryDate != null && expiryDateCheckDigit != null && expiryDateCheckDigit.length() == 1) {
            if (!mrzValidationService.validateChecksum(expiryDate, expiryDateCheckDigit.charAt(0))) {
                errorMessages.append("Expiry Date Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing Expiry Date or its check digit for TD2 validation. ");
            isValid = false;
        }

        // Composite Checksum
        // For TD2, the composite check digit typically covers specific segments of line 2.
        String mrzLine2 = ocrData.get("mrzLine2");
        String compositeCheckDigit = ocrData.get("compositeCheckDigit");

        if (mrzLine2 != null && compositeCheckDigit != null && compositeCheckDigit.length() == 1) {
            String compositeString = mrzLine2.substring(0, 10) + // Document Number + Check
                                     mrzLine2.substring(13, 20) + // Birth Date + Check
                                     mrzLine2.substring(21, 28) + // Expiry Date + Check
                                     mrzLine2.substring(28, 35);  // Optional fields
            if (!mrzValidationService.validateChecksum(compositeString, compositeCheckDigit.charAt(0))) {
                errorMessages.append("Composite Check Digit invalid. ");
                isValid = false;
            }
        } else {
            errorMessages.append("Missing data for Composite Check Digit validation for TD2. ");
            isValid = false;
        }

        return isValid;
    }
}
