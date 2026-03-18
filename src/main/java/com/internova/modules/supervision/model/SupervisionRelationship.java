package com.internova.modules.supervision.model;

import com.internova.modules.student.model.Student;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "supervision_relationships")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SupervisionRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private AcademicSupervisor supervisor;

    @Column(name = "academic_year")
    private String academicYear;

    private String semester;
}
