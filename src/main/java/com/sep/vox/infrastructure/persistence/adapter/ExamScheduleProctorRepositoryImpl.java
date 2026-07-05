package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamScheduleProctorRepository;

@Repository
public class ExamScheduleProctorRepositoryImpl implements ExamScheduleProctorRepository {

    private final SpringDataExamScheduleProctorRepository springDataExamScheduleProctorRepository;

    public ExamScheduleProctorRepositoryImpl(SpringDataExamScheduleProctorRepository springDataExamScheduleProctorRepository) {
        this.springDataExamScheduleProctorRepository = springDataExamScheduleProctorRepository;
    }

    @Override
    public boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID studentId) {
        return springDataExamScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, studentId);
    }
    
}
