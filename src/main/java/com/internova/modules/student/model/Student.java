package com.internova.modules.student.model;

import com.internova.core.model.User;
import com.internova.modules.organization.model.Department;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student extends User {

    @Column(name = "student_id_number", unique = true, nullable = false)
    private String studentIdNumber;

    private String course;

    @Column(name = "cv_url")
    private String cvUrl;

    @Column(name = "profile_completion")
    private Double profileCompletion = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
