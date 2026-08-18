package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.DuplicatedException;
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
            throw new DuplicatedException("Giám thị đã được phân công cho ca này");
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

    @Override
    public void deleteByScheduleIdIn(java.util.Collection<UUID> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return;
        }
        springDataExamScheduleProctorRepository.deleteByScheduleIdIn(scheduleIds);
    }

    @Override
    public List<UUID> findScheduleIdsByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return List.of();
        }
        return springDataExamScheduleProctorRepository.findByTeacherIdAndScheduleIdIn(teacherId, scheduleIds)
            .stream()
            .map(e -> e.getScheduleId())
            .distinct()
            .toList();
    }

    @Override
    public boolean existsOverlappingAssignment(UUID teacherId, Instant start, Instant end, UUID excludeScheduleId) {
        // Ca thi chưa đặt giờ thì không có gì để so — coi như không vướng, đúng như cách
        // ExamScheduleRoomValidator bỏ qua khi thiếu dữ liệu.
        if (teacherId == null || start == null || end == null) {
            return false;
        }
        return springDataExamScheduleProctorRepository
            .countOverlappingAssignments(teacherId, start, end, excludeScheduleId) > 0;
    }

    @Override
    public List<ProctorScheduleConflict> findConflictsForTeachers(
            Collection<UUID> teacherIds, Instant start, Instant end, UUID excludeScheduleId) {
        if (teacherIds == null || teacherIds.isEmpty() || start == null || end == null) {
            return List.of();
        }
        return springDataExamScheduleProctorRepository
            .findOverlappingAssignments(teacherIds, start, end, excludeScheduleId).stream()
            .map(row -> new ProctorScheduleConflict(
                (UUID) row[0], (UUID) row[1], (Instant) row[2], (Instant) row[3]))
            .toList();
    }
}
