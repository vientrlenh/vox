package com.sep.vox.infrastructure.event.internal.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.event.ExamMemberAssignedEvent;
import com.sep.vox.application.port.output.PushNotificationPort;

class ExamMemberAssignedPushListenerTests {

    @Test
    void should_send_push_to_assigned_teacher() {
        var pushNotificationPort = mock(PushNotificationPort.class);
        var listener = new ExamMemberAssignedPushListener(pushNotificationPort);

        var examId = UUID.randomUUID();
        var teacherUserId = UUID.randomUUID();
        listener.handle(new ExamMemberAssignedEvent(examId, teacherUserId, "CHAIR"));

        verify(pushNotificationPort).sendToUser(
            teacherUserId,
            "Phân công mới",
            "Bạn vừa được giao quản lí một kì kiểm tra mới",
            Map.of("type", "exam_member_assigned", "examId", examId.toString())
        );
    }
}
