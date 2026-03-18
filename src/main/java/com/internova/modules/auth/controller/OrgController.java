package com.internova.modules.auth.controller;

import com.internova.modules.organization.dto.DepartmentResponse;
import com.internova.modules.organization.dto.FacultyResponse;
import com.internova.modules.organization.dto.UniversityResponse;
import com.internova.modules.organization.model.Department;
import com.internova.modules.organization.model.Faculty;
import com.internova.modules.vacancy.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    /**
     * Get all universities
     * GET /api/v1/organizations/universities
     */
    @GetMapping("/universities")
    public ResponseEntity<?> getAllUniversities() {
        List<UniversityResponse> responses = orgService.getAllUniversities().stream()
                .map(u -> new UniversityResponse(u.getId(), u.getName(), u.getLocation(), null))
                .toList();

        return ResponseEntity.ok(Map.of(
                "universities", responses,
                "count", responses.size()));
    }

    /**
     * Create a new university (admin only)
     * POST /api/v1/organizations/universities
     */
    @PostMapping("/universities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUniversity(@Valid @RequestBody UniversityResponse request) {
        // This would require UniversityRepository - stub implementation
        return ResponseEntity.ok(Map.of("message", "University created"));
    }

    /**
     * Get university by ID
     * GET /api/v1/organizations/universities/{id}
     */
    @GetMapping("/universities/{id}")
    public ResponseEntity<?> getUniversity(@PathVariable UUID id) {
        var university = orgService.getUniversityById(id);

        if (university == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new UniversityResponse(
                university.getId(),
                university.getName(),
                university.getLocation(),
                null));
    }

    /**
     * Get all faculties for a university
     * GET /api/v1/organizations/universities/{universityId}/faculties
     */
    @GetMapping("/universities/{universityId}/faculties")
    public ResponseEntity<?> getFacultiesByUniversity(@PathVariable UUID universityId) {
        List<Faculty> faculties = orgService.getFacultiesByUniversity(universityId);

        List<FacultyResponse> responses = faculties.stream()
                .map(f -> new FacultyResponse(f.getId(), f.getName(), null, f.getUniversity().getName()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "faculties", responses,
                "count", responses.size()));
    }

    /**
     * Get all departments for a faculty
     * GET /api/v1/organizations/faculties/{facultyId}/departments
     */
    @GetMapping("/faculties/{facultyId}/departments")
    public ResponseEntity<?> getDepartmentsByFaculty(@PathVariable UUID facultyId) {
        List<Department> departments = orgService.getDepartmentsByFaculty(facultyId);

        List<DepartmentResponse> responses = departments.stream()
                .map(d -> new DepartmentResponse(
                        d.getId(),
                        d.getName(),
                        d.getCode() != null ? d.getCode() : "",
                        d.getDescription() != null ? d.getDescription() : "",
                        d.getFaculty().getName(),
                        d.getFaculty().getUniversity().getName()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "departments", responses,
                "count", responses.size()));
    }

    /**
     * Get all departments
     * GET /api/v1/organizations/departments
     */
    @GetMapping("/departments")
    public ResponseEntity<?> getAllDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Department> departments = orgService.getAllDepartments(pageable);

        List<DepartmentResponse> responses = departments.getContent().stream()
                .map(d -> new DepartmentResponse(
                        d.getId(),
                        d.getName(),
                        d.getCode() != null ? d.getCode() : "",
                        d.getDescription() != null ? d.getDescription() : "",
                        d.getFaculty().getName(),
                        d.getFaculty().getUniversity().getName()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "departments", responses,
                "totalPages", departments.getTotalPages(),
                "totalElements", departments.getTotalElements()));
    }

    /**
     * Get department by ID
     * GET /api/v1/organizations/departments/{id}
     */
    @GetMapping("/departments/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable UUID id) {
        Department department = orgService.getDepartmentById(id);

        DepartmentResponse response = new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode() != null ? department.getCode() : "",
                department.getDescription() != null ? department.getDescription() : "",
                department.getFaculty().getName(),
                department.getFaculty().getUniversity().getName());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new faculty (admin only)
     * POST /api/v1/organizations/faculties
     */
    @PostMapping("/faculties")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createFaculty(@Valid @RequestBody FacultyResponse request) {
        // This would require FacultyRepository - stub implementation
        return ResponseEntity.ok(Map.of("message", "Faculty created"));
    }

    /**
     * Create a new department (admin only)
     * POST /api/v1/organizations/departments
     */
    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createDepartment(@Valid @RequestBody DepartmentResponse request) {
        // This would require DepartmentRepository save - stub implementation
        return ResponseEntity.ok(Map.of("message", "Department created"));
    }
}