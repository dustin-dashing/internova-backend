package com.internova.integration.brain.controller;

import com.internova.integration.brain.dto.ResumeParseResponseDto;
import com.internova.modules.student.model.Student;
import com.internova.modules.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/brain")
@RequiredArgsConstructor
@Slf4j
public class BrainWebhookController {

    private final StudentRepository studentRepository;

    @PostMapping("/resume-parsed/{studentId}")
    @Transactional
    public ResponseEntity<Void> handleResumeParsed(
            @PathVariable UUID studentId,
            @RequestBody ResumeParseResponseDto payload) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (payload.email() != null && !payload.email().equalsIgnoreCase(student.getEmail())) {
            log.warn("Resume parse email mismatch for student {}: payload={}, stored={}",
                    studentId,
                    payload.email(),
                    student.getEmail());
        }

        if (payload.profileCompletionSuggestion() != null) {
            double suggestedCompletion = Math.max(student.getProfileCompletion(), payload.profileCompletionSuggestion());
            student.setProfileCompletion(suggestedCompletion);
        }

        studentRepository.save(student);
        log.info("Received parsed resume data for student: {}", studentId);
        return ResponseEntity.ok().build();
    }
}