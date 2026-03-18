package com.internova.modules.vacancy.service;

import com.internova.modules.logbook.model.LogbookEntry;
import com.internova.modules.logbook.repository.LogbookRepository;
import com.internova.modules.student.model.Student;
import com.internova.modules.student.repository.StudentRepository;
import com.internova.modules.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final StudentRepository studentRepository;
    private final LogbookRepository logbookRepository;
    private final VacancyRepository vacancyRepository;

    public Map<String, Object> getStudentPlacementStats(UUID departmentId) {
        Map<String, Object> stats = new HashMap<>();

        List<Student> students = studentRepository.findByDepartmentId(departmentId);
        stats.put("totalStudents", students.size());
        stats.put("placedStudents", students.stream()
                .filter(s -> s.getProfileCompletion() >= 60.0)
                .count());
        stats.put("profileCompletionRate",
                students.stream().mapToDouble(Student::getProfileCompletion).average().orElse(0.0));

        return stats;
    }

    public Map<String, Object> getLogbookComplianceStats(UUID departmentId) {
        Map<String, Object> stats = new HashMap<>();

        List<Student> students = studentRepository.findByDepartmentId(departmentId);
        Map<String, Long> complianceMap = new HashMap<>();
        complianceMap.put("GREEN", 0L);
        complianceMap.put("YELLOW", 0L);
        complianceMap.put("RED", 0L);

        for (Student student : students) {
            List<LogbookEntry> logs = logbookRepository.findByStudentIdOrderByEntryDateDesc(student.getId());
            String status = logs.isEmpty() ? "RED" : "GREEN";
            complianceMap.put(status, complianceMap.get(status) + 1);
        }

        stats.putAll(complianceMap);
        return stats;
    }

    public Map<String, Object> getVacancyStats(UUID departmentId) {
        Map<String, Object> stats = new HashMap<>();

        long totalVacancies = vacancyRepository.count();
        long activeVacancies = vacancyRepository.findAll().stream()
                .filter(v -> v.getIsActive())
                .count();

        stats.put("totalVacancies", totalVacancies);
        stats.put("activeVacancies", activeVacancies);

        return stats;
    }
}
