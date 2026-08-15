package com.sep.vox.interfaces.kafka.dto;

/**
 * Payload topic {@code exam.alert.raised} do vox-streaming phát (xem domain.AlertRaisedEvent bên đó).
 *
 * <p>{@code participantId} là candidateId, {@code sessionId} là id phiên thi. Hai trường này từng bị
 * gán cùng một giá trị ở phía phát; vox-streaming nay tra bù lại trước khi phát, nhưng dữ liệu cũ
 * trên topic thì vẫn còn nguyên như thế -- nên phía đọc không được coi participantId là chắc chắn.
 */
public record AlertRaisedEventDto(
    String eventId,
    String raisedAt,
    String source,
    String sessionId,
    String participantId,
    String streamId,
    String streamType,
    String alertType,
    String detail,
    Double confidence,
    Long sequenceNo,
    String level,
    String capturedAt
) {
}
