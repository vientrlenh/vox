package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ConflictException;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamScheduleProctorMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamScheduleProctorRepository;

@Repository
public class ExamScheduleProctorRepositoryImpl implements ExamScheduleProctorRepository {

    private final SpringDataExamScheduleProctorRepository springDataExamScheduleProctorRepository;

    public ExamScheduleProctorRepositoryImpl(SpringDataExamScheduleProctorRepository springDataExamScheduleProctorRepository) {
        this.springDataExamScheduleProctorRepository = springDataExamScheduleProctorRepository;
    }

    @Override
    public ExamScheduleProctor save(ExamScheduleProctor proctor) {
        try {
            var saved = springDataExamScheduleProctorRepository.save(ExamScheduleProctorMapper.toJpa(proctor));
            return ExamScheduleProctorMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Giám thị đã được phân công cho ca này");
        }
    }

    @Override
    public Optional<ExamScheduleProctor> findById(UUID id) {
        return springDataExamScheduleProctorRepository.findById(id)
            .map(ExamScheduleProctorMapper::toDomain);
    }

    @Override
    public List<ExamScheduleProctor> findByScheduleId(UUID scheduleId) {
        return springDataExamScheduleProctorRepository.findByScheduleId(scheduleId).stream()
            .map(ExamScheduleProctorMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId) {
        return springDataExamScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, teacherId);
    }

    @Override
    public long countByScheduleId(UUID scheduleId) {
        return springDataExamScheduleProctorRepository.countByScheduleId(scheduleId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamScheduleProctorRepository.deleteById(id);
    }
}
