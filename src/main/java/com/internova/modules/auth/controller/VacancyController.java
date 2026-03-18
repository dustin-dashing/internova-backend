package com.internova.modules.auth.controller;

import com.internova.core.model.User;
import com.internova.modules.company.model.Company;
import com.internova.modules.vacancy.dto.VacancyRequest;
import com.internova.modules.vacancy.dto.VacancyResponse;
import com.internova.modules.vacancy.model.Vacancy;
import com.internova.modules.vacancy.service.VacancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vacancies")
@RequiredArgsConstructor
public class VacancyController {

    private final VacancyService vacancyService;

    /**
     * Post a new vacancy (company only)
     * POST /api/v1/vacancies
     */
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_REP')")
    public ResponseEntity<?> postVacancy(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VacancyRequest request) {

        if (!(user instanceof Company company)) {
            return ResponseEntity.badRequest().body("Only companies can post vacancies");
        }

        Vacancy vacancy = vacancyService.postVacancy(
                company,
                request.getTitle(),
                request.getDescription(),
                request.getRequirements());

        VacancyResponse response = new VacancyResponse(
                vacancy.getId(),
                vacancy.getTitle(),
                vacancy.getDescription(),
                vacancy.getRequirements(),
                vacancy.getLocation(),
                vacancy.getCompany().getCompanyName(),
                vacancy.getCompany().getIndustry(),
                vacancy.getIsActive(),
                vacancy.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Get vacancy discovery feed for a department
     * GET /api/v1/vacancies/feed/{departmentId}
     */
    @GetMapping("/feed/{departmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getVacancyFeed(@PathVariable UUID departmentId) {
        List<Vacancy> vacancies = vacancyService.getDiscoveryFeed(departmentId);

        List<VacancyResponse> responses = vacancies.stream()
                .map(v -> new VacancyResponse(
                        v.getId(),
                        v.getTitle(),
                        v.getDescription(),
                        v.getRequirements(),
                        v.getLocation(),
                        v.getCompany().getCompanyName(),
                        v.getCompany().getIndustry(),
                        v.getIsActive(),
                        v.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get all vacancies (public)
     * GET /api/v1/vacancies
     */
    @GetMapping
    public ResponseEntity<?> getAllVacancies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Vacancy> vacancies = vacancyService.getAllVacancies(pageable);

        List<VacancyResponse> responses = vacancies.getContent().stream()
                .map(v -> new VacancyResponse(
                        v.getId(),
                        v.getTitle(),
                        v.getDescription(),
                        v.getRequirements(),
                        v.getLocation(),
                        v.getCompany().getCompanyName(),
                        v.getCompany().getIndustry(),
                        v.getIsActive(),
                        v.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "vacancies", responses,
                "totalPages", vacancies.getTotalPages(),
                "totalElements", vacancies.getTotalElements()));
    }

    /**
     * Get single vacancy by ID
     * GET /api/v1/vacancies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVacancyById(@PathVariable UUID id) {
        Vacancy vacancy = vacancyService.getVacancyById(id);

        VacancyResponse response = new VacancyResponse(
                vacancy.getId(),
                vacancy.getTitle(),
                vacancy.getDescription(),
                vacancy.getRequirements(),
                vacancy.getLocation(),
                vacancy.getCompany().getCompanyName(),
                vacancy.getCompany().getIndustry(),
                vacancy.getIsActive(),
                vacancy.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Search vacancies by keyword
     * GET /api/v1/vacancies/search
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchVacancies(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Vacancy> vacancies = vacancyService.searchVacancies(keyword, pageable);

        List<VacancyResponse> responses = vacancies.getContent().stream()
                .map(v -> new VacancyResponse(
                        v.getId(),
                        v.getTitle(),
                        v.getDescription(),
                        v.getRequirements(),
                        v.getLocation(),
                        v.getCompany().getCompanyName(),
                        v.getCompany().getIndustry(),
                        v.getIsActive(),
                        v.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "vacancies", responses,
                "totalPages", vacancies.getTotalPages(),
                "totalElements", vacancies.getTotalElements()));
    }

    /**
     * Update vacancy (company owner only)
     * PUT /api/v1/vacancies/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_REP')")
    public ResponseEntity<?> updateVacancy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody VacancyRequest request) {

        Vacancy vacancy = vacancyService.getVacancyById(id);

        // Verify ownership
        if (!vacancy.getCompany().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        Vacancy updated = vacancyService.updateVacancy(id, request);

        VacancyResponse response = new VacancyResponse(
                updated.getId(),
                updated.getTitle(),
                updated.getDescription(),
                updated.getRequirements(),
                updated.getLocation(),
                updated.getCompany().getCompanyName(),
                updated.getCompany().getIndustry(),
                updated.getIsActive(),
                updated.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate vacancy (company owner only)
     * DELETE /api/v1/vacancies/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY_REP')")
    public ResponseEntity<?> deactivateVacancy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        Vacancy vacancy = vacancyService.getVacancyById(id);

        // Verify ownership
        if (!vacancy.getCompany().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        vacancyService.deactivateVacancy(id);
        return ResponseEntity.ok(Map.of("message", "Vacancy deactivated successfully"));
    }
}