package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamCandidateResultMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamCandidateResultRepository;

@Repository
public class ExamCandidateResultRepositoryImpl implements ExamCandidateResultRepository {

    private final SpringDataExamCandidateResultRepository springDataExamCandidateResultRepository;

    public ExamCandidateResultRepositoryImpl(
            SpringDataExamCandidateResultRepository springDataExamCandidateResultRepository) {
        this.springDataExamCandidateResultRepository = springDataExamCandidateResultRepository;
    }

    @Override
    public PageResult<ExamCandidateResult> findByStudentId(UUID studentId, int page, int size) {
        var result = springDataExamCandidateResultRepository.findByStudentId(studentId, PageRequest.of(page, size));
        return new PageResult<>(
            result.getContent().stream().map(ExamCandidateResultMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}
