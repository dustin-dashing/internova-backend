package com.internova.modules.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class FacultyResponse {
    private UUID id;
    private String name;
    private String description;
    private String universityName;
}
