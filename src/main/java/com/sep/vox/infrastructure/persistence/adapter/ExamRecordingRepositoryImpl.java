package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamRecordingMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamRecordingRepository;

@Repository
public class ExamRecordingRepositoryImpl implements ExamRecordingRepository {

    private final SpringDataExamRecordingRepository springDataExamRecordingRepository;

    public ExamRecordingRepositoryImpl(SpringDataExamRecordingRepository springDataExamRecordingRepository) {
        this.springDataExamRecordingRepository = springDataExamRecordingRepository;
    }

    @Override
    public List<ExamRecording> findByExamSessionId(UUID examSessionId) {
        return springDataExamRecordingRepository.findByExamSessionId(examSessionId)
            .stream()
            .map(ExamRecordingMapper::toDomain)
            .toList();
    }
    
}
