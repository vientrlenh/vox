package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.ExamRecordingEntry;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.ExamItemResponseMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamItemResponseRepository;

@Repository
public class ExamRecordingRepositoryImpl implements ExamRecordingRepository {

    private final SpringDataExamItemResponseRepository springDataExamItemResponseRepository;

    public ExamRecordingRepositoryImpl(
            SpringDataExamItemResponseRepository springDataExamItemResponseRepository) {
        this.springDataExamItemResponseRepository = springDataExamItemResponseRepository;
    }

    @Override
    public List<ExamRecordingEntry> findByStudentIdWithAudio(UUID studentId) {
        return springDataExamItemResponseRepository.findByStudentIdWithAudio(studentId).stream()
            .map(row -> new ExamRecordingEntry(
                ExamItemResponseMapper.toDomain((ExamItemResponseJpaEntity) row[0]),
                (UUID) row[1]))
            .toList();
    }
}
