package com.internova.modules.logbook.repository;

import com.internova.modules.logbook.model.LogbookEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LogbookRepository extends JpaRepository<LogbookEntry, UUID> {
    List<LogbookEntry> findByStudentIdOrderByEntryDateDesc(UUID studentId);
    Optional<LogbookEntry> findByStudentIdAndEntryDate(UUID studentId, LocalDate entryDate);
    List<LogbookEntry> findTop5ByStudentIdOrderByEntryDateDesc(UUID studentId);
}
