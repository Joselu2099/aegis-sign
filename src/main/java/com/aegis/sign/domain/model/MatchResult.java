package com.aegis.sign.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchResult {
    private final boolean isMatch;
    private final double similarityScore;
    private final double livenessScore;
    private final double confidence;
}
