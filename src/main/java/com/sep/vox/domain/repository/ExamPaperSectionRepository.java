package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamPaperSection;

public interface ExamPaperSectionRepository {
    ExamPaperSection save(ExamPaperSection section);
    Optional<ExamPaperSection> findById(UUID id);
    List<ExamPaperSection> findByPaperId(UUID paperId);
    List<ExamPaperSection> findByPaperIdIn(Collection<UUID> paperIds);
    void deleteById(UUID id);
}
