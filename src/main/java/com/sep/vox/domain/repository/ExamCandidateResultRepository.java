package com.sep.vox.domain.repository;

import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateResult;

public interface ExamCandidateResultRepository {
    PageResult<ExamCandidateResult> findByStudentId(UUID studentId, int page, int size);
}
