package com.internova.modules.logbook.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LogbookResponse {
    private UUID id;
    private LocalDate entryDate;
    private String content;
    private String tags;
    private LocalDateTime submittedAt;
    private boolean isStamped;
    private String supervisorRemarks;
    private String status; // "ON_TIME" or "LATE" (calculated)
}
