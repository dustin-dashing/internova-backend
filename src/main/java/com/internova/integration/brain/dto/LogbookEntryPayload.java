package com.internova.integration.brain.dto;

public record LogbookEntryPayload(
        String content,
        Double hoursWorked
) {
}