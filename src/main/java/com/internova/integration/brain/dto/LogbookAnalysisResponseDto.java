package com.internova.integration.brain.dto;

import java.util.List;

public record LogbookAnalysisResponseDto(
        String sentiment,
        String summary,
        Double riskScore,
        List<String> flaggedConcerns
) {
}