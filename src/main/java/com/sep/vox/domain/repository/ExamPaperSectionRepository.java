package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamPaperSection;

public interface ExamPaperSectionRepository {
    ExamPaperSection save(ExamPaperSection section);
    List<ExamPaperSection> findByPaperId(UUID paperId);
    void deleteById(UUID id);
}
