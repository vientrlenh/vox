package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamItemResponseRepository;

@Repository
public class ExamItemResponseRepositoryImpl implements ExamItemResponseRepository {

    private final SpringDataExamItemResponseRepository springDataExamItemResponseRepository;

    public ExamItemResponseRepositoryImpl(SpringDataExamItemResponseRepository springDataExamItemResponseRepository) {
        this.springDataExamItemResponseRepository = springDataExamItemResponseRepository;
    }

    @Override
    public int sumDurationSecondsBySessionId(UUID sessionId) {
        return springDataExamItemResponseRepository.sumDurationSecondsBySessionId(sessionId);
    }
}