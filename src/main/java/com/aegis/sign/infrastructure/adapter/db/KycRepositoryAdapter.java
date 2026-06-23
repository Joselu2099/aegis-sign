package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.KycSession;
import com.aegis.sign.domain.port.KycRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.KycSessionEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.KycSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KycRepositoryAdapter implements KycRepositoryPort {

    private final KycSessionRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<KycSession> save(KycSession session) {
        return repository.save(toEntity(session))
                .map(this::toDomain);
    }

    @Override
    public Mono<KycSession> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    private KycSessionEntity toEntity(KycSession session) {
        String extractedData = null;
        try {
            Map<String, Object> data = new HashMap<>(session.getDocumentMetadata());
            data.put("signerId", session.getSignerId());
            data.put("mrzValid", session.isMrzValid());
            data.put("mrzValidationErrorMessage", session.getMrzValidationErrorMessage());
            data.put("biometricValid", session.isBiometricValid());
            data.put("biometricValidationErrorMessage", session.getBiometricValidationErrorMessage());
            extractedData = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            // Log error
        }

        return KycSessionEntity.builder()
                .id(session.getId())
                .status(mapStatusToDb(session.getStatus()))
                .extractedData(extractedData != null ? io.r2dbc.postgresql.codec.Json.of(extractedData) : null)
                .biometricScore(session.getFaceMatchScore())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
    }

    private KycSession toDomain(KycSessionEntity entity) {
        Map<String, String> metadata = new HashMap<>();
        String signerId = null;
        boolean mrzValid = false;
        String mrzValidationErrorMessage = null;
        boolean biometricValid = false;
        String biometricValidationErrorMessage = null;

        if (entity.getExtractedData() != null) {
            try {
                Map<String, Object> data = objectMapper.readValue(entity.getExtractedData().asString(), new TypeReference<>() {});
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if ("signerId".equals(key)) {
                        signerId = (String) value;
                    } else if ("mrzValid".equals(key)) {
                        mrzValid = (Boolean) value;
                    } else if ("mrzValidationErrorMessage".equals(key)) {
                        mrzValidationErrorMessage = (String) value;
                    } else if ("biometricValid".equals(key)) {
                        biometricValid = (Boolean) value;
                    } else if ("biometricValidationErrorMessage".equals(key)) {
                        biometricValidationErrorMessage = (String) value;
                    } else {
                        metadata.put(key, String.valueOf(value));
                    }
                }
            } catch (JsonProcessingException e) {
                // Log error
            }
        }

        return KycSession.builder()
                .id(entity.getId())
                .status(mapStatusToDomain(entity.getStatus(), mrzValid, biometricValid))
                .documentMetadata(metadata)
                .faceMatchScore(entity.getBiometricScore())
                .signerId(signerId)
                .mrzValid(mrzValid)
                .mrzValidationErrorMessage(mrzValidationErrorMessage)
                .biometricValid(biometricValid)
                .biometricValidationErrorMessage(biometricValidationErrorMessage)
                .build();
    }

    private String mapStatusToDb(KycSession.KycStatus status) {
        if (status == null) return "PENDING_DOCUMENTS";
        return switch (status) {
            case PENDING_DOCUMENTS -> "PENDING_DOCUMENTS";
            case PROCESSING -> "PROCESSING";
            case MANUAL_REVIEW -> "MANUAL_REVIEW";
            case APPROVED -> "APPROVED";
            case REJECTED -> "REJECTED";
            case MRZ_FAILED, BIOMETRIC_FAILED -> "FAILED";
        };
    }

    private KycSession.KycStatus mapStatusToDomain(String status, boolean mrzValid, boolean biometricValid) {
        if (status == null) return KycSession.KycStatus.PENDING_DOCUMENTS;
        return switch (status) {
            case "APPROVED" -> KycSession.KycStatus.APPROVED;
            case "REJECTED" -> KycSession.KycStatus.REJECTED;
            case "PROCESSING" -> KycSession.KycStatus.PROCESSING;
            case "MANUAL_REVIEW" -> KycSession.KycStatus.MANUAL_REVIEW;
            case "FAILED" -> {
                if (!mrzValid) yield KycSession.KycStatus.MRZ_FAILED;
                if (!biometricValid) yield KycSession.KycStatus.BIOMETRIC_FAILED;
                yield KycSession.KycStatus.REJECTED;
            }
            default -> KycSession.KycStatus.PENDING_DOCUMENTS;
        };
    }
}
