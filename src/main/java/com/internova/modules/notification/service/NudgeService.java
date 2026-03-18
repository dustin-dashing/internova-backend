package com.internova.modules.notification.service;

import com.internova.modules.application.enums.ApplicationStatus;
import com.internova.modules.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NudgeService {

    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    // Runs every day at midnight.
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkForGhosting() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        applicationRepository.findAllByStatusAndUpdatedAtBefore(ApplicationStatus.APPLIED, oneWeekAgo)
                .forEach(app -> notificationService.send(
                        app.getVacancy().getCompany(),
                        "Action Required: Pending Application",
                        "Student " + app.getStudent().getEmail() + " has been waiting for 7 days. Please review."
                ));
    }
}
