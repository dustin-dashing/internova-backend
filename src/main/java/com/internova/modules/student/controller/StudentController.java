package com.internova.modules.student.controller;

import com.internova.core.model.User;
import com.internova.modules.student.model.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    /**
     * Get student profile
     * GET /api/v1/students/profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        if (!(user instanceof Student student)) {
            return ResponseEntity.badRequest().body("Only students can access this endpoint");
        }

        return ResponseEntity.ok(Map.of(
                "id", student.getId(),
                "email", student.getEmail(),
                "studentIdNumber", student.getStudentIdNumber(),
                "course", student.getCourse(),
                "cvUrl", student.getCvUrl(),
                "profileCompletion", student.getProfileCompletion(),
                "department", student.getDepartment().getName(),
                "status", student.getStatus()));
    }

    /**
     * Update student profile
     * PUT /api/v1/students/profile
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> profileUpdate) {

        if (!(user instanceof Student student)) {
            return ResponseEntity.badRequest().body("Only students can access this endpoint");
        }

        // Update fields if provided
        if (profileUpdate.containsKey("course")) {
            student.setCourse((String) profileUpdate.get("course"));
        }
        if (profileUpdate.containsKey("cvUrl")) {
            student.setCvUrl((String) profileUpdate.get("cvUrl"));
        }

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    /**
     * Get profile completion percentage
     * GET /api/v1/students/profile-completion
     */
    @GetMapping("/profile-completion")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getProfileCompletion(@AuthenticationPrincipal User user) {
        if (!(user instanceof Student student)) {
            return ResponseEntity.badRequest().body("Only students can access this endpoint");
        }

        // Calculate what's missing
        Map<String, Boolean> completionStatus = Map.of(
                "hasBasicInfo", true,
                "hasCourse", student.getCourse() != null && !student.getCourse().isEmpty(),
                "hasCv", student.getCvUrl() != null && !student.getCvUrl().isEmpty(),
                "hasProfilePicture", false // Can be extended based on actual requirements
        );

        return ResponseEntity.ok(Map.of(
                "completionPercentage", student.getProfileCompletion(),
                "completionStatus", completionStatus,
                "message", student.getProfileCompletion() >= 60.0 ? "You can now apply to vacancies"
                        : "Complete your profile to apply to vacancies"));
    }
}
