package com.internova.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FacultyRequest {
    @NotNull
    private UUID universityId;

    @NotBlank
    private String name;

    private String description;
}
