package com.sep.vox.infrastructure.event.internal.listener;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.ExamCandidatesBulkAddedEvent;
import com.sep.vox.application.port.output.PushNotificationPort;

@Component
public class ExamCandidatesBulkAddedPushListener {

    private static final String TITLE = "Bài kiểm tra mới";
    private static final String BODY = "Bạn có bài kiểm tra mới";

    private final PushNotificationPort pushNotificationPort;

    public ExamCandidatesBulkAddedPushListener(PushNotificationPort pushNotificationPort) {
        this.pushNotificationPort = pushNotificationPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ExamCandidatesBulkAddedEvent event) {
        pushNotificationPort.sendToUsers(
            event.studentUserIds(),
            TITLE,
            BODY,
            Map.of("type", "exam_candidate_added", "examId", event.examId().toString())
        );
    }
}
