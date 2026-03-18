package com.internova.integration.brain.dto;

public record ResumeParseRequest(
        String studentId,
        String fileUrl
) {
}