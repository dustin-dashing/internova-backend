package com.internova.integration.brain.client;

import com.internova.integration.brain.dto.LogbookAnalysisResponseDto;
import com.internova.integration.brain.dto.LogbookEntryPayload;
import com.internova.integration.brain.dto.ResumeParseRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class BrainClient {

    private final RestClient restClient;

    public BrainClient(@Value("${internova.brain.url:http://localhost:8000}") String brainUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(brainUrl)
                .build();
    }

    public LogbookAnalysisResponseDto analyzeLogbook(String content) {
        LogbookEntryPayload request = new LogbookEntryPayload(content, 8.0);

        return restClient.post()
                .uri("/api/logbook/analyze")
                .body(request)
                .retrieve()
                .body(LogbookAnalysisResponseDto.class);
    }

    @Async
    @Retryable(
            retryFor = {ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void triggerResumeParse(String studentId, String fileUrl) {
        log.info("Triggering AI parse for student {}, file: {}", studentId, fileUrl);
        ResumeParseRequest request = new ResumeParseRequest(studentId, fileUrl);

        restClient.post()
                .uri("/api/resume/parse")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Recover
    public void recoverResumeParse(ResourceAccessException exception, String studentId, String fileUrl) {
        log.error(
                "CRITICAL: Python AI Brain is unreachable after retries for student {}. File {} is stored but unparsed.",
                studentId,
                fileUrl,
                exception
        );
    }
}