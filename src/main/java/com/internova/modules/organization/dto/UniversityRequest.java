package com.internova.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UniversityRequest {
    @NotBlank
    private String name;

    private String location;

    private String website;
}
