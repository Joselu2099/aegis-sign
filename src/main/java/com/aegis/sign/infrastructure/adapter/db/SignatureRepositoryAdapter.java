package com.aegis.sign.infrastructure.adapter.db;

import com.aegis.sign.domain.model.Signature;
import com.aegis.sign.domain.port.SignatureRepositoryPort;
import com.aegis.sign.infrastructure.adapter.db.entity.SignatureEntity;
import com.aegis.sign.infrastructure.adapter.db.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SignatureRepositoryAdapter implements SignatureRepositoryPort {

    private final SignatureRepository repository;

    @Override
    public Mono<Signature> save(Signature signature) {
        return repository.save(toEntity(signature))
                .map(this::toDomain);
    }

    @Override
    public Mono<Signature> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<Page<Signature>> findByContractId(UUID contractId, Pageable pageable) {
        return repository.findByContractId(contractId, pageable)
                .collectList()
                .map(entities -> new PageImpl<>(
                        entities.stream().map(this::toDomain).toList(),
                        pageable,
                        entities.size()
                ));
    }

    private SignatureEntity toEntity(Signature signature) {
        return SignatureEntity.builder()
                .id(signature.getId())
                .contractId(signature.getContractId())
                .signerInfo(io.r2dbc.postgresql.codec.Json.of("{\"signerId\":\"" + signature.getSignerId() + "\"}"))
                .x509CertificateSn(signature.getCertificateThumbprint())
                .timestamp(signature.getTimestamp())
                .build();
    }

    private Signature toDomain(SignatureEntity entity) {
        return Signature.builder()
                .id(entity.getId())
                .contractId(entity.getContractId())
                .signerId(entity.getSignerInfo() != null ? entity.getSignerInfo().asString() : null)
                .certificateThumbprint(entity.getX509CertificateSn())
                .timestamp(entity.getTimestamp())
                .build();
    }
}

