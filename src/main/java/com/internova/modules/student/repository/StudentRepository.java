package com.internova.modules.student.repository;

import com.internova.modules.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findByDepartmentId(UUID departmentId);
}