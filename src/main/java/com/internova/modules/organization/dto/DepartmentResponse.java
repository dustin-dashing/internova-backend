package com.internova.modules.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class DepartmentResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String facultyName;
    private String universityName;
}
