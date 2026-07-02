package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamPaperSectionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamPaperSectionRepository;

@Repository
public class ExamPaperSectionRepositoryImpl implements ExamPaperSectionRepository {

    private final SpringDataExamPaperSectionRepository springDataExamPaperSectionRepository;

    public ExamPaperSectionRepositoryImpl(SpringDataExamPaperSectionRepository springDataExamPaperSectionRepository) {
        this.springDataExamPaperSectionRepository = springDataExamPaperSectionRepository;
    }

    @Override
    public ExamPaperSection save(ExamPaperSection section) {
        var saved = springDataExamPaperSectionRepository.save(ExamPaperSectionMapper.toJpa(section));
        return ExamPaperSectionMapper.toDomain(saved);
    }

    @Override
    public List<ExamPaperSection> findByPaperId(UUID paperId) {
        return springDataExamPaperSectionRepository.findByPaperIdOrderByOrderAsc(paperId).stream()
            .map(ExamPaperSectionMapper::toDomain)
            .toList();
    }
}
