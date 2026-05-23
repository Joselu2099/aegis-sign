package com.aegis.sign.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Signature {
    private UUID id;
    private String signerId;
    private LocalDateTime timestamp;
    private String hash;
    private String certificateThumbprint;
    private UUID contractId;
}
