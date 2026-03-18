package com.internova.modules.vacancy.repository;

import com.internova.modules.vacancy.model.Vacancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VacancyRepository extends JpaRepository<Vacancy, UUID> {

    @Query("""
                SELECT v FROM Vacancy v
                LEFT JOIN com.internova.modules.company.model.DepartmentPartnership dp
                    ON v.company.id = dp.company.id
                    AND dp.department.id = :deptId
                WHERE v.isActive = true
                ORDER BY
                    CASE
                        WHEN dp.partnershipType = 'EXCLUSIVE' THEN 1
                        WHEN dp.partnershipType = 'PREFERRED' THEN 2
                        WHEN dp.partnershipType IS NOT NULL THEN 3
                        ELSE 4
                    END ASC,
                    v.createdAt DESC
            """)
    List<Vacancy> findRankedVacanciesForDepartment(@Param("deptId") UUID deptId);

    Page<Vacancy> findByIsActiveTrue(Pageable pageable);

    Page<Vacancy> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String titleKeyword, String descriptionKeyword, Pageable pageable);
}
