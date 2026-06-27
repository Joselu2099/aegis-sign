package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.exception.PersistenceSerializationException;
import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.SignatureEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.SignatureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SignatureRepositoryAdapter implements SignatureRepositoryPort {

    private final SignatureRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Signature> save(Signature signature) {
        return toEntity(signature)
                .flatMap(repository::save)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Signature> findById(UUID id) {
        return repository.findById(id)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Page<Signature>> findByContractId(UUID contractId, Pageable pageable) {
        return repository.findByContractId(contractId, pageable)
                .flatMap(this::toDomain)
                .collectList()
                .zipWith(repository.countByContractId(contractId))
                .map(tuple -> new PageImpl<>(
                        tuple.getT1(),
                        pageable,
                        tuple.getT2()
                ));
    }

    private Mono<SignatureEntity> toEntity(Signature signature) {
        return Mono.fromCallable(() -> {
            Map<String, String> signerInfoMap = new HashMap<>();
            signerInfoMap.put("signerId", signature.getSignerId());
            String signerInfoJson;
            try {
                signerInfoJson = objectMapper.writeValueAsString(signerInfoMap);
            } catch (JsonProcessingException e) {
                throw new PersistenceSerializationException(
                        "Failed to serialize signerInfo for signature " + signature.getId(), e);
            }
            return SignatureEntity.builder()
                    .id(signature.getId())
                    .contractId(signature.getContractId())
                    .signerInfo(Json.of(signerInfoJson))
                    .x509CertificateSn(signature.getCertificateThumbprint())
                    .timestamp(signature.getTimestamp())
                    .build();
        });
    }

    private Mono<Signature> toDomain(SignatureEntity entity) {
        return Mono.fromCallable(() -> {
            String signerId = null;
            if (entity.getSignerInfo() != null) {
                Map<String, String> signerInfoMap;
                try {
                    signerInfoMap = objectMapper.readValue(
                            entity.getSignerInfo().asString(),
                            new TypeReference<Map<String, String>>() {});
                } catch (JsonProcessingException e) {
                    throw new PersistenceSerializationException(
                            "Failed to deserialize signerInfo for signature " + entity.getId(), e);
                }
                signerId = signerInfoMap.get("signerId");
            }
            return Signature.builder()
                    .id(entity.getId())
                    .contractId(entity.getContractId())
                    .signerId(signerId)
                    .certificateThumbprint(entity.getX509CertificateSn())
                    .timestamp(entity.getTimestamp())
                    .build();
        });
    }
}
