package com.sep.vox.application.projection.dto;

import java.util.UUID;

/** Giáo viên có thể nhận bài, kèm tải hiện tại (số bài ASSIGNED đang giữ). */
public record AssignableTeacherInfo(
    UUID id,
    String name,
    long load
) {
}
