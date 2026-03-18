package com.internova.modules.vacancy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VacancyRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private String requirements;

    private String location;
}
