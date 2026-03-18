package com.internova.modules.student.controller;

import com.internova.core.service.StorageService;
import com.internova.integration.brain.client.BrainClient;
import com.internova.modules.student.model.Student;
import com.internova.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/students/me")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StorageService storageService;
    private final BrainClient brainClient;
    private final StudentRepository studentRepository;

    @PostMapping("/resume")
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public ResponseEntity<?> uploadResume(
            @AuthenticationPrincipal Student student,
            @RequestParam("file") MultipartFile file) {
        String fileUrl = storageService.store(file, "resumes");

        student.setCvUrl(fileUrl);
        studentRepository.save(student);

        brainClient.triggerResumeParse(student.getId().toString(), fileUrl);

        return ResponseEntity.accepted().body(Map.of(
                "message", "Resume uploaded successfully. AI analysis is processing in the background.",
                "cv_url", fileUrl
        ));
    }
}