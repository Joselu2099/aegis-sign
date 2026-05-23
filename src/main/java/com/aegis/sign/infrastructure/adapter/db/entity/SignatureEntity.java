package com.aegis.sign.infrastructure.adapter.db.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("signatures")
public class SignatureEntity {
    @Id
    private UUID id;
    private UUID contractId;
    private String signerInfo; // We'll map signerId to this JSON
    private String x509CertificateSn;
    private LocalDateTime timestamp;
}
