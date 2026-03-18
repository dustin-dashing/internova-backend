package com.internova.modules.auth.controller;

import com.internova.core.model.User;
import com.internova.modules.student.model.Student;
import com.internova.modules.vacancy.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Get placement statistics for a department
     * GET /api/v1/reports/placements/{departmentId}
     */
    @GetMapping("/placements/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public ResponseEntity<?> getPlacementStats(@PathVariable UUID departmentId) {
        Map<String, Object> stats = reportService.getStudentPlacementStats(departmentId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get logbook compliance statistics for a department
     * GET /api/v1/reports/compliance/{departmentId}
     */
    @GetMapping("/compliance/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public ResponseEntity<?> getComplianceStats(@PathVariable UUID departmentId) {
        Map<String, Object> stats = reportService.getLogbookComplianceStats(departmentId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get vacancy statistics
     * GET /api/v1/reports/vacancies
     */
    @GetMapping("/vacancies")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_REP')")
    public ResponseEntity<?> getVacancyStats() {
        Map<String, Object> stats = reportService.getVacancyStats(null);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get comprehensive dashboard report for a department
     * GET /api/v1/reports/dashboard/{departmentId}
     */
    @GetMapping("/dashboard/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT_HEAD')")
    public ResponseEntity<?> getDashboardReport(@PathVariable UUID departmentId) {
        Map<String, Object> placementStats = reportService.getStudentPlacementStats(departmentId);
        Map<String, Object> complianceStats = reportService.getLogbookComplianceStats(departmentId);
        Map<String, Object> vacancyStats = reportService.getVacancyStats(departmentId);

        return ResponseEntity.ok(Map.of(
                "placements", placementStats,
                "compliance", complianceStats,
                "vacancies", vacancyStats,
                "timestamp", System.currentTimeMillis()));
    }

    /**
     * Get student's personal report
     * GET /api/v1/reports/my-report
     */
    @GetMapping("/my-report")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentReport(@AuthenticationPrincipal User user) {
        if (!(user instanceof Student student)) {
            return ResponseEntity.badRequest().body("Only students can access personal reports");
        }

        Map<String, Object> report = Map.of(
                "studentId", student.getId(),
                "email", student.getEmail(),
                "profileCompletion", student.getProfileCompletion(),
                "department", student.getDepartment().getName(),
                "reportGeneratedAt", System.currentTimeMillis());

        return ResponseEntity.ok(report);
    }

    /**
     * Export statistics (admin only)
     * GET /api/v1/reports/export/{departmentId}
     */
    @GetMapping("/export/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportStats(@PathVariable UUID departmentId) {
        Map<String, Object> placementStats = reportService.getStudentPlacementStats(departmentId);
        Map<String, Object> complianceStats = reportService.getLogbookComplianceStats(departmentId);
        Map<String, Object> vacancyStats = reportService.getVacancyStats(departmentId);

        return ResponseEntity.ok(Map.of(
                "exportFormat", "JSON",
                "departmentId", departmentId,
                "placements", placementStats,
                "compliance", complianceStats,
                "vacancies", vacancyStats,
                "exportedAt", System.currentTimeMillis()));
    }
}