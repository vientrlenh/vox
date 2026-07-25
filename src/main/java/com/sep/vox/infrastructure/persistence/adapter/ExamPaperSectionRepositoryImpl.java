package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    public Optional<ExamPaperSection> findById(UUID id) {
        return springDataExamPaperSectionRepository.findById(id).map(ExamPaperSectionMapper::toDomain);
    }

    @Override
    public List<ExamPaperSection> findByPaperId(UUID paperId) {
        return springDataExamPaperSectionRepository.findByPaperIdOrderByOrderAsc(paperId).stream()
            .map(ExamPaperSectionMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamPaperSection> findByPaperIdIn(Collection<UUID> paperIds) {
        if (paperIds.isEmpty()) {
            return List.of();
        }
        return springDataExamPaperSectionRepository.findByPaperIdInOrderByOrderAsc(paperIds).stream()
            .map(ExamPaperSectionMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamPaperSectionRepository.deleteById(id);
    }
}
