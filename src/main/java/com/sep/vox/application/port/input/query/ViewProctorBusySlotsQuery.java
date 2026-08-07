package com.sep.vox.application.port.input.query;

import java.util.List;
import java.util.UUID;

/** Trong nhóm {@code teacherIds}, ai đang vướng lịch với khung giờ của ca {@code scheduleId}. */
public record ViewProctorBusySlotsQuery(
    UUID scheduleId,
    List<UUID> teacherIds
) {
}
