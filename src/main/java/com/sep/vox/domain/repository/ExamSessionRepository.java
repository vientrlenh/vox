package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSession;

public interface ExamSessionRepository {
    List<ExamSession> findByCandidateId(UUID candidateId);
}
