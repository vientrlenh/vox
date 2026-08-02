package com.sep.vox.application.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.port.input.service.TopicSuggestionService;

@Component
public class TopicSuggestionSessionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        TopicSuggestionSessionListener.class
    );

    private final TopicSuggestionService service;

    public TopicSuggestionSessionListener(
            TopicSuggestionService service) {
        this.service = service;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionEnded(PracticeSessionEndedEvent event) {
        try {
            service.refreshSuggestions(event.studentId());
        } catch (Exception exception) {
            LOGGER.warn(
                "Không thể làm mới gợi ý chủ đề cho học sinh {}",
                event.studentId(),
                exception
            );
        }
    }
}
