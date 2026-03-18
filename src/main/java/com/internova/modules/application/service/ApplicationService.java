package com.internova.modules.application.service;

import com.internova.modules.application.enums.ApplicationStatus;
import com.internova.modules.application.model.Application;
import com.internova.modules.application.repository.ApplicationRepository;
import com.internova.modules.student.model.Student;
import com.internova.modules.vacancy.model.Vacancy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Transactional
    public Application apply(Student student, Vacancy vacancy) {
        if (student.getProfileCompletion() < 60.0) {
            throw new RuntimeException("Profile must be at least 60% complete to apply.");
        }

        if (applicationRepository.existsByStudentIdAndVacancyId(student.getId(), vacancy.getId())) {
            throw new RuntimeException("You already applied to this vacancy.");
        }

        Application application = new Application();
        application.setStudent(student);
        application.setVacancy(vacancy);
        return applicationRepository.save(application);
    }

    @Transactional
    public void updateStatus(UUID applicationId, ApplicationStatus newStatus) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Single-placement guardrail: one accepted offer at a time.
        if (newStatus == ApplicationStatus.ACCEPTED) {
            long activeAccepted = applicationRepository.countByStudentIdAndStatus(
                    app.getStudent().getId(),
                    ApplicationStatus.ACCEPTED);
            if (activeAccepted > 0 && app.getStatus() != ApplicationStatus.ACCEPTED) {
                throw new RuntimeException("Student already has an accepted placement.");
            }
        }

        app.setStatus(newStatus);
        applicationRepository.save(app);
    }

    @Transactional(readOnly = true)
    public Page<Application> getStudentApplications(UUID studentId, Pageable pageable) {
        return applicationRepository.findByStudentId(studentId, pageable);
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @Transactional
    public void withdrawApplication(UUID applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (app.getStatus() == ApplicationStatus.ACCEPTED) {
            throw new RuntimeException("Cannot withdraw an accepted application");
        }

        applicationRepository.delete(app);
    }
}
