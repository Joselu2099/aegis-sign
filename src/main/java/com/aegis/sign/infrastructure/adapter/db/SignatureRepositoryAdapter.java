package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.SignatureEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.SignatureRepository;
import com.aegis.sign.infrastructure.adapter.web.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SignatureRepositoryAdapter implements SignatureRepositoryPort {

    private final SignatureRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Signature> save(Signature signature) {
        return repository.save(toEntity(signature))
                .map(this::toDomain);
    }

    @Override
    public Mono<Signature> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Signature not found: " + id)));
    }

    private SignatureEntity toEntity(Signature signature) {
        return SignatureEntity.builder()
                .id(signature.getId())
                .contractId(signature.getContractId())
                .signerInfo("{\"signerId\":\"" + signature.getSignerId() + "\"}")
                .x509CertificateSn(signature.getCertificateThumbprint())
                .timestamp(signature.getTimestamp())
                .build();
    }

    private String extractSignerId(String signerInfoJson) {
        if (signerInfoJson == null) return null;
        try {
            return objectMapper.readTree(signerInfoJson).path("signerId").asText(null);
        } catch (JsonProcessingException e) {
            return signerInfoJson;
        }
    }

    private Signature toDomain(SignatureEntity entity) {
        return Signature.builder()
                .id(entity.getId())
                .contractId(entity.getContractId())
                .signerId(extractSignerId(entity.getSignerInfo()))
                .certificateThumbprint(entity.getX509CertificateSn())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
