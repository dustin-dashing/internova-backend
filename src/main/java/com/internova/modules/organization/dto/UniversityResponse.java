package com.internova.modules.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UniversityResponse {
    private UUID id;
    private String name;
    private String location;
    private String website;
}
