package com.aegis.sign.infrastructure.adapter.db.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("contracts")
public class ContractEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    @Column("template_id")
    private String templateId;

    private String status;

    @Column("document_hash_sha256")
    private String documentHashSha256;

    @Column("minio_uri")
    private String minioUri;

    @Column("signer_ids")
    private String signerIds;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
