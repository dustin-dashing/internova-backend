package com.internova.modules.logbook.service;

import com.internova.modules.student.model.Student;
import com.internova.modules.logbook.model.LogbookEntry;
import com.internova.modules.logbook.repository.LogbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogbookService {

    private final LogbookRepository logbookRepository;

    @Transactional
    public LogbookEntry submitEntry(Student student, LocalDate entryDate, String content, String tags) {
        // Rule 1: Prevent duplicate entries for the same date
        if (logbookRepository.findByStudentIdAndEntryDate(student.getId(), entryDate).isPresent()) {
            throw new RuntimeException("Log already exists for this date.");
        }

        // Rule 2: Enforce 48-hour submission window
        long hoursSinceWorkDate = ChronoUnit.HOURS.between(entryDate.atStartOfDay(), LocalDateTime.now());
        if (hoursSinceWorkDate > 48 + 24) { // 48 hours from the end of the entry day
            throw new RuntimeException("Submission window closed for this date (48-hour rule).");
        }

        LogbookEntry entry = new LogbookEntry();
        entry.setStudent(student);
        entry.setEntryDate(entryDate);
        entry.setContent(content);
        entry.setTags(tags);

        return logbookRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<LogbookEntry> getLogsByStudent(UUID studentId) {
        return logbookRepository.findByStudentIdOrderByEntryDateDesc(studentId);
    }

    @Transactional(readOnly = true)
    public String calculateComplianceStatus(UUID studentId) {
        List<LogbookEntry> recentLogs = logbookRepository.findTop5ByStudentIdOrderByEntryDateDesc(studentId);

        if (recentLogs.isEmpty())
            return "RED"; // No logs submitted at all

        long missingDays = ChronoUnit.DAYS.between(recentLogs.get(0).getEntryDate(), LocalDate.now());

        if (missingDays > 7)
            return "RED"; // Hasn't logged in over a week
        if (missingDays > 3)
            return "YELLOW"; // Starting to slack
        return "GREEN"; // Active and compliant
    }

    @Transactional(readOnly = true)
    public LogbookEntry getLogbookEntryById(UUID entryId) {
        return logbookRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Logbook entry not found"));
    }

    @Transactional
    public LogbookEntry updateEntry(UUID entryId, String content, String tags) {
        LogbookEntry entry = getLogbookEntryById(entryId);
        entry.setContent(content);
        entry.setTags(tags);
        return logbookRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(UUID entryId) {
        logbookRepository.deleteById(entryId);
    }
}
