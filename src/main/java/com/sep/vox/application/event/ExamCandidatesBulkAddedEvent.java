package com.sep.vox.application.event;

import java.util.List;
import java.util.UUID;

public record ExamCandidatesBulkAddedEvent(
    UUID examId,
    List<UUID> studentUserIds
) {

}
