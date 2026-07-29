package com.sep.vox.infrastructure.event.internal.listener;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.ExamMemberAssignedEvent;
import com.sep.vox.application.port.output.PushNotificationPort;

@Component
public class ExamMemberAssignedPushListener {

    private static final String TITLE = "Phân công mới";
    private static final String BODY = "Bạn vừa được giao quản lí một kì kiểm tra mới";

    private final PushNotificationPort pushNotificationPort;

    public ExamMemberAssignedPushListener(PushNotificationPort pushNotificationPort) {
        this.pushNotificationPort = pushNotificationPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ExamMemberAssignedEvent event) {
        pushNotificationPort.sendToUser(
            event.teacherUserId(),
            TITLE,
            BODY,
            Map.of("type", "exam_member_assigned", "examId", event.examId().toString())
        );
    }
}
