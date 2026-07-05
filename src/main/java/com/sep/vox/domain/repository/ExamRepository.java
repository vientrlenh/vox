package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.Exam;

public interface ExamRepository {
    Optional<Exam> findById(UUID id);
    Exam save(Exam exam);
    List<Exam> findByIdIn(Collection<UUID> ids);
}
