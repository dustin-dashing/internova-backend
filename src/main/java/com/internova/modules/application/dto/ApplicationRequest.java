package com.internova.modules.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ApplicationRequest {
    @NotNull
    private UUID vacancyId;
}
