package com.internova.integration.brain.dto;

public record ResumeParseResponseDto(
        String fullName,
        String email,
        SkillSetDto skills,
        Double yearsOfExperience,
        String summary,
        Double profileCompletionSuggestion
) {
}