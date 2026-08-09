package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.UUID;

/** Ứng viên kỳ thi tập trung, dùng để chọn ra kỳ thi gần thời điểm hiện tại nhất */
record CandidateExamProjection(
    UUID id, 
    String code, 
    String name, 
    String status, 
    Instant openAt, 
    Instant closeAt) {
}
