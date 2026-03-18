package com.internova.modules.organization.repository;

import com.internova.modules.organization.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
}
