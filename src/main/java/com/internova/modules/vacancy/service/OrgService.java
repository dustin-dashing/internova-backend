package com.internova.modules.vacancy.service;

import com.internova.modules.organization.model.Department;
import com.internova.modules.organization.model.Faculty;
import com.internova.modules.organization.model.University;
import com.internova.modules.organization.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<University> getAllUniversities() {
        // This method stub will be implemented with UniversityRepository when added
        return List.of();
    }

    @Transactional(readOnly = true)
    public University getUniversityById(UUID id) {
        // This method stub will be implemented with UniversityRepository
        return null;
    }

    @Transactional(readOnly = true)
    public List<Faculty> getFacultiesByUniversity(UUID universityId) {
        // This method stub will be implemented with FacultyRepository when added
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<Department> getDepartmentsByFaculty(UUID facultyId) {
        // This method stub will be implemented when needed
        return List.of();
    }

    @Transactional(readOnly = true)
    public Department getDepartmentById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    @Transactional(readOnly = true)
    public Page<Department> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable);
    }
}