package com.internova.modules.vacancy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class VacancyResponse {
    private UUID id;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private String companyName;
    private String industry;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
