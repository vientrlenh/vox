package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamRepository;

public class ExamRepositoryImpl implements ExamRepository {

    private final SpringDataExamRepository springDataExamRepository;

    public ExamRepositoryImpl(SpringDataExamRepository springDataExamRepository) {
        this.springDataExamRepository = springDataExamRepository;
    }

    @Override
    public Optional<Exam> findById(UUID id) {
        return springDataExamRepository.findById(id)
            .map(ExamMapper::toDomain);
    }

    @Override
    public Exam save(Exam exam) {
        var entity = ExamMapper.toJpa(exam);
        var saved = springDataExamRepository.save(entity);
        return ExamMapper.toDomain(saved);
    }
    
}
