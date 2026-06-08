package com.aegis.sign.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {
    private UUID id;
    private String contentHash;
    private ContractStatus status;
    private String uri;
    private String templateId;
    private List<String> signerIds;

    public void markAsSigned() {
        if (this.status == ContractStatus.SIGNED) {
            throw new IllegalStateException("Contract is already signed");
        }
        this.status = ContractStatus.SIGNED;
    }

    public enum ContractStatus {
        DRAFT, PREPARED, PENDING_SIGNATURE, SIGNED, CANCELLED, EXPIRED, REVOKED
    }
}
