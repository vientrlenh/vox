package com.sep.vox.infrastructure.worker.personalization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Component
public class TopicSuggestionRefreshJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        TopicSuggestionRefreshJob.class
    );

    private final TopicSuggestionService service;

    public TopicSuggestionRefreshJob(
            TopicSuggestionService service) {
        this.service = service;
    }

    @Scheduled(
        fixedDelayString = "${app.practice.topic-refresh-delay-ms:3600000}",
        initialDelayString = "${app.practice.topic-refresh-initial-delay-ms:60000}"
    )
    public void refreshDueStudents() {
        for (var studentId : service.studentsDueForSuggestionRefresh(25)) {
            try {
                service.refreshSuggestions(studentId);
            } catch (Exception exception) {
                LOGGER.warn(
                    "Không thể làm mới gợi ý chủ đề nền cho học sinh {}",
                    studentId,
                    exception
                );
            }
        }
    }
}
