package com.internova.integration.brain.dto;

import java.util.List;

public record SkillSetDto(
        List<String> hardSkills,
        List<String> softSkills,
        List<String> tools
) {
}