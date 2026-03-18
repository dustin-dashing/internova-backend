package com.internova.modules.company.model;

import com.internova.modules.organization.model.Department;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "department_partnerships")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DepartmentPartnership {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "partnership_type")
    private String partnershipType; // e.g., 'EXCLUSIVE', 'PREFERRED'

    @Column(name = "established_at")
    private LocalDate establishedAt = LocalDate.now();
}
