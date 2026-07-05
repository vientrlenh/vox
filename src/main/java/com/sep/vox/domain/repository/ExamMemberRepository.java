package com.sep.vox.domain.repository;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamMemberRole;

public interface ExamMemberRepository {
    boolean existsByExamIdAndUserIdAndRole(UUID examId, UUID userId, ExamMemberRole role);
}
