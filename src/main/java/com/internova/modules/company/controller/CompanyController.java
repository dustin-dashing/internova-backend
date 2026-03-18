package com.internova.modules.company.controller;

import com.internova.core.model.User;
import com.internova.modules.company.model.Company;
import com.internova.modules.vacancy.dto.VacancyResponse;
import com.internova.modules.vacancy.model.Vacancy;
import com.internova.modules.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final VacancyRepository vacancyRepository;

    /**
     * Get company profile
     * GET /api/v1/company/profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('COMPANY_REP')")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        if (!(user instanceof Company company)) {
            return ResponseEntity.badRequest().body("Only companies can access this endpoint");
        }

        return ResponseEntity.ok(Map.of(
                "id", company.getId(),
                "email", company.getEmail(),
                "companyName", company.getCompanyName(),
                "registrationNumber", company.getRegistrationNumber(),
                "industry", company.getIndustry(),
                "isVerified", company.getIsVerified(),
                "status", company.getStatus()));
    }

    /**
     * Get company's posted vacancies
     * GET /api/v1/company/vacancies
     */
    @GetMapping("/vacancies")
    @PreAuthorize("hasRole('COMPANY_REP')")
    public ResponseEntity<?> getCompanyVacancies(@AuthenticationPrincipal User user) {
        if (!(user instanceof Company company)) {
            return ResponseEntity.badRequest().body("Only companies can access this endpoint");
        }

        List<Vacancy> vacancies = vacancyRepository.findAll().stream()
                .filter(v -> v.getCompany().getId().equals(company.getId()))
                .toList();

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

        return ResponseEntity.ok(Map.of(
                "vacancies", responses,
                "count", responses.size()));
    }

    /**
     * Get verification status
     * GET /api/v1/company/verification-status
     */
    @GetMapping("/verification-status")
    @PreAuthorize("hasRole('COMPANY_REP')")
    public ResponseEntity<?> getVerificationStatus(@AuthenticationPrincipal User user) {
        if (!(user instanceof Company company)) {
            return ResponseEntity.badRequest().body("Only companies can access this endpoint");
        }

        return ResponseEntity.ok(Map.of(
                "isVerified", company.getIsVerified(),
                "status", company.getStatus(),
                "message",
                company.getIsVerified() ? "Your company is verified" : "Your company is pending verification"));
    }
}
