package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/** Xoá cả nhóm thí sinh khỏi kỳ thi trong MỘT transaction — dùng cho màn tick chọn nhiều thí sinh. */
public record BulkDeleteExamCandidatesCommand(
    UUID examId,
    List<UUID> candidateIds
) {
}
