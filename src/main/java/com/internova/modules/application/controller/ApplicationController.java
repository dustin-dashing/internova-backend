package com.internova.modules.application.controller;

import com.internova.core.model.User;
import com.internova.modules.application.dto.ApplicationRequest;
import com.internova.modules.application.dto.ApplicationResponse;
import com.internova.modules.application.enums.ApplicationStatus;
import com.internova.modules.application.model.Application;
import com.internova.modules.application.service.ApplicationService;
import com.internova.modules.student.model.Student;
import com.internova.modules.vacancy.model.Vacancy;
import com.internova.modules.vacancy.repository.VacancyRepository;
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
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final VacancyRepository vacancyRepository;

    /**
     * Student applies to a vacancy
     * POST /api/v1/applications
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> applyToVacancy(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ApplicationRequest request) {

        if (!(user instanceof Student student)) {
            return ResponseEntity.badRequest().body("Only students can apply");
        }

        Vacancy vacancy = vacancyRepository.findById(request.getVacancyId())
                .orElseThrow(() -> new RuntimeException("Vacancy not found"));

        Application application = applicationService.apply(student, vacancy);

        ApplicationResponse response = new ApplicationResponse(
                application.getId(),
                vacancy.getTitle(),
                vacancy.getCompany().getCompanyName(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all applications for the authenticated student
     * GET /api/v1/applications
     */
    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyApplications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (!(user instanceof Student student)) {
            return ResponseEntity.badRequest().body("Only students can view applications");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationService.getStudentApplications(student.getId(), pageable);

        List<ApplicationResponse> responses = applications.getContent().stream()
                .map(app -> new ApplicationResponse(
                        app.getId(),
                        app.getVacancy().getTitle(),
                        app.getVacancy().getCompany().getCompanyName(),
                        app.getStatus(),
                        app.getAppliedAt(),
                        app.getUpdatedAt()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "applications", responses,
                "totalPages", applications.getTotalPages(),
                "totalElements", applications.getTotalElements()));
    }

    /**
     * Get a specific application by ID
     * GET /api/v1/applications/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getApplicationById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        Application application = applicationService.getApplicationById(id);

        // Verify ownership
        if (!application.getStudent().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        ApplicationResponse response = new ApplicationResponse(
                application.getId(),
                application.getVacancy().getTitle(),
                application.getVacancy().getCompany().getCompanyName(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw an application
     * DELETE /api/v1/applications/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> withdrawApplication(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        Application application = applicationService.getApplicationById(id);

        // Verify ownership
        if (!application.getStudent().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        applicationService.withdrawApplication(id);
        return ResponseEntity.ok(Map.of("message", "Application withdrawn successfully"));
    }

    /**
     * Update application status (for company/admin)
     * PATCH /api/v1/applications/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COMPANY_REP', 'ADMIN')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable UUID id,
            @RequestParam ApplicationStatus status) {

        applicationService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Application status updated"));
    }
}
