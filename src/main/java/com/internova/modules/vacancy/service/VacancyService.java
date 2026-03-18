package com.internova.modules.vacancy.service;

import com.internova.modules.company.model.Company;
import com.internova.modules.vacancy.dto.VacancyRequest;
import com.internova.modules.vacancy.model.Vacancy;
import com.internova.modules.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VacancyService {

    private final VacancyRepository vacancyRepository;

    @Transactional
    public Vacancy postVacancy(Company company, String title, String description, String requirements) {
        if (!company.getIsVerified()) {
            throw new RuntimeException("Company must be verified by an admin to post vacancies.");
        }

        Vacancy vacancy = new Vacancy();
        vacancy.setCompany(company);
        vacancy.setTitle(title);
        vacancy.setDescription(description);
        vacancy.setRequirements(requirements);

        return vacancyRepository.save(vacancy);
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getDiscoveryFeed(UUID departmentId) {
        return vacancyRepository.findRankedVacanciesForDepartment(departmentId);
    }

    @Transactional(readOnly = true)
    public Page<Vacancy> getAllVacancies(Pageable pageable) {
        return vacancyRepository.findByIsActiveTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Vacancy getVacancyById(UUID id) {
        return vacancyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vacancy not found"));
    }

    @Transactional(readOnly = true)
    public Page<Vacancy> searchVacancies(String keyword, Pageable pageable) {
        return vacancyRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword,
                pageable);
    }

    @Transactional
    public Vacancy updateVacancy(UUID id, VacancyRequest request) {
        Vacancy vacancy = getVacancyById(id);

        vacancy.setTitle(request.getTitle());
        vacancy.setDescription(request.getDescription());
        vacancy.setRequirements(request.getRequirements());
        vacancy.setLocation(request.getLocation());

        return vacancyRepository.save(vacancy);
    }

    @Transactional
    public void deactivateVacancy(UUID id) {
        Vacancy vacancy = getVacancyById(id);
        vacancy.setIsActive(false);
        vacancyRepository.save(vacancy);
    }
}
