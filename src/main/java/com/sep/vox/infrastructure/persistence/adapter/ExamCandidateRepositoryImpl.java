package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamCandidateRepository;

@Repository
public class ExamCandidateRepositoryImpl implements ExamCandidateRepository {

    private final SpringDataExamCandidateRepository springDataExamCandidateRepository;

    public ExamCandidateRepositoryImpl(SpringDataExamCandidateRepository springDataExamCandidateRepository) {
        this.springDataExamCandidateRepository = springDataExamCandidateRepository;
    }

    @Override
    public boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId) {
        return springDataExamCandidateRepository.existsByScheduleIdAndStudentId(scheduleId, studentId);
    }
    
}
