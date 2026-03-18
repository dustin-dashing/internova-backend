package com.internova.modules.logbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LogbookRequest {
    @NotNull
    private LocalDate entryDate;

    @NotBlank
    private String content;

    private String tags;
}
