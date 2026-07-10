package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemResponse;

public interface ExamItemResponseRepository {
    Optional<ExamItemResponse> findById(UUID id);
    boolean existsById(UUID id);
    ExamItemResponse save(ExamItemResponse response);
    List<ExamItemResponse> findBySessionId(UUID sessionId);
}
