package com.internova.modules.application.repository;

import com.internova.modules.application.enums.ApplicationStatus;
import com.internova.modules.application.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    boolean existsByStudentIdAndVacancyId(UUID studentId, UUID vacancyId);

    long countByStudentIdAndStatus(UUID studentId, ApplicationStatus status);

    List<Application> findAllByStatusAndUpdatedAtBefore(ApplicationStatus status, LocalDateTime timestamp);

    Page<Application> findByStudentId(UUID studentId, Pageable pageable);
}
