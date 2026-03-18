package com.internova.modules.logbook.controller;

import com.internova.modules.logbook.dto.LogbookResponse;
import com.internova.modules.logbook.model.LogbookEntry;
import com.internova.modules.logbook.service.LogbookService;
import com.internova.modules.student.model.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/logbooks")
@RequiredArgsConstructor
public class LogbookController {

    private final LogbookService logbookService;

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitLog(
            @AuthenticationPrincipal Student student,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody Map<String, String> request) {
        
        LogbookEntry entry = logbookService.submitEntry(
                student, 
                date, 
                request.get("content"), 
                request.get("tags")
        );
        
        return ResponseEntity.ok(Map.of("message", "Log submitted successfully", "id", entry.getId()));
    }

    @GetMapping("/my-logs")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LogbookResponse>> getMyLogs(@AuthenticationPrincipal Student student) {
        return ResponseEntity.ok(
                logbookService.getLogsByStudent(student.getId()).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList())
        );
    }

    private LogbookResponse mapToResponse(LogbookEntry entry) {
        return LogbookResponse.builder()
                .id(entry.getId())
                .entryDate(entry.getEntryDate())
                .content(entry.getContent())
                .tags(entry.getTags())
                .submittedAt(entry.getSubmittedAt())
                .isStamped(entry.getIsStamped())
                .supervisorRemarks(entry.getSupervisorRemarks())
                .status(entry.getSubmittedAt().toLocalDate().isAfter(entry.getEntryDate().plusDays(2)) ? "LATE" : "ON_TIME")
                .build();
    }
}
