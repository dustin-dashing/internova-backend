package com.internova.modules.supervision.model;

import com.internova.core.model.User;
import com.internova.modules.organization.model.Department;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "academic_supervisors")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AcademicSupervisor extends User {

    @Column(name = "staff_id", unique = true, nullable = false)
    private String staffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
