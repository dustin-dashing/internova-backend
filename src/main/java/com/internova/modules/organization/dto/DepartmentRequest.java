package com.internova.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DepartmentRequest {
    @NotNull
    private UUID facultyId;

    @NotBlank
    private String name;

    private String code;

    private String description;
}
