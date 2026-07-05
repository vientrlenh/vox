package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamScheduleMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamScheduleRepository;

@Repository
public class ExamScheduleRepositoryImpl implements ExamScheduleRepository {

    private final SpringDataExamScheduleRepository springDataExamScheduleRepository;

    public ExamScheduleRepositoryImpl(SpringDataExamScheduleRepository springDataExamScheduleRepository) {
        this.springDataExamScheduleRepository = springDataExamScheduleRepository;
    }

    @Override
    public Optional<ExamSchedule> findById(UUID id) {
        return springDataExamScheduleRepository.findById(id)
            .map(ExamScheduleMapper::toDomain);
    }

    @Override
    public List<ExamSchedule> findByExamId(UUID examId) {
        return springDataExamScheduleRepository.findByExamId(examId)
            .stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSchedule> findByExamIdAndInSchedule(UUID examId, OffsetDateTime now) {
        return springDataExamScheduleRepository.findByExamIdAndInSchedule(examId, now)
            .stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSchedule> findByIdInAndInSchedule(Collection<UUID> ids, OffsetDateTime now) {
        return springDataExamScheduleRepository.findByIdInAndInSchedule(ids, now)
            .stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }
    
}
