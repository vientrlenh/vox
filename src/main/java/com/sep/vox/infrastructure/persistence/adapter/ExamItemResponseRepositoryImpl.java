package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamItemResponseMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamItemResponseRepository;

@Repository
public class ExamItemResponseRepositoryImpl implements ExamItemResponseRepository {

    private final SpringDataExamItemResponseRepository springDataExamItemResponseRepository;

    public ExamItemResponseRepositoryImpl(SpringDataExamItemResponseRepository springDataExamItemResponseRepository) {
        this.springDataExamItemResponseRepository = springDataExamItemResponseRepository;
    }

    @Override
    public Optional<ExamItemResponse> findById(UUID id) {
        return springDataExamItemResponseRepository.findById(id)
            .map(ExamItemResponseMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataExamItemResponseRepository.existsById(id);
    }

    @Override
    public ExamItemResponse save(ExamItemResponse response) {
        var saved = springDataExamItemResponseRepository.save(ExamItemResponseMapper.toJpa(response));
        return ExamItemResponseMapper.toDomain(saved);
    }

    @Override
    public List<ExamItemResponse> findBySessionId(UUID sessionId) {
        return springDataExamItemResponseRepository.findBySessionId(sessionId).stream()
            .map(ExamItemResponseMapper::toDomain)
            .toList();
    }
}
