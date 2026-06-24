package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.Exam;

public interface ExamRepository {
    Optional<Exam> findById(UUID id);
    Exam save(Exam exam);
}
