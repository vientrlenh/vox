package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/** {@code scheduleId} null nghĩa là gỡ cả nhóm khỏi ca thi, giống endpoint xếp từng thí sinh. */
public record BulkAssignExamCandidateScheduleCommand(
    UUID examId,
    List<UUID> candidateIds,
    UUID scheduleId
) {
}
