package com.internova.modules.logbook.model;

import com.internova.modules.student.model.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "logbook_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LogbookEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate entryDate; // The day the work was done

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String tags; // Comma-separated or JSON for skills tracked

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "is_stamped")
    private Boolean isStamped = false;

    private LocalDateTime stampedAt;

    @Column(name = "supervisor_remarks")
    private String supervisorRemarks;
}
