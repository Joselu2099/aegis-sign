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
                .zipWith(repository.countByContractId(contractId))
                .map(tuple -> new PageImpl<>(
                        tuple.getT1().stream().map(this::toDomain).toList(),
                        pageable,
                        tuple.getT2()
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
                .signerId(extractSignerId(entity.getSignerInfo()))
                .certificateThumbprint(entity.getX509CertificateSn())
                .timestamp(entity.getTimestamp())
                .build();
    }

    private String extractSignerId(io.r2dbc.postgresql.codec.Json json) {
        if (json == null) {
            return null;
        }
        String str = json.asString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"signerId\"\\s*:\\s*\"([^\"]+)\"").matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return str;
    }
}

