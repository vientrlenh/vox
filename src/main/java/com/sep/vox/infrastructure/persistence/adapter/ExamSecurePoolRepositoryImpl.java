package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamSecurePool;
import com.sep.vox.domain.repository.ExamSecurePoolRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamSecurePoolMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamSecurePoolRepository;

@Repository
public class ExamSecurePoolRepositoryImpl implements ExamSecurePoolRepository {

    private final SpringDataExamSecurePoolRepository springDataExamSecurePoolRepository;

    public ExamSecurePoolRepositoryImpl(SpringDataExamSecurePoolRepository springDataExamSecurePoolRepository) {
        this.springDataExamSecurePoolRepository = springDataExamSecurePoolRepository;
    }

    @Override
    public ExamSecurePool save(ExamSecurePool pool) {
        var saved = springDataExamSecurePoolRepository.save(ExamSecurePoolMapper.toJpa(pool));
        return ExamSecurePoolMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamSecurePool> findById(UUID id) {
        return springDataExamSecurePoolRepository.findById(id)
            .map(ExamSecurePoolMapper::toDomain);
    }

    @Override
    public Optional<ExamSecurePool> findByExamId(UUID examId) {
        return springDataExamSecurePoolRepository.findByExamId(examId)
            .map(ExamSecurePoolMapper::toDomain);
    }
}
