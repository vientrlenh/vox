package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentScheduleProctorMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentScheduleProctorRepository;

@Repository
public class AssessmentScheduleProctorRepositoryImpl implements AssessmentScheduleProctorRepository {

    private final SpringDataAssessmentScheduleProctorRepository springDataAssessmentScheduleProctorRepository;

    public AssessmentScheduleProctorRepositoryImpl(SpringDataAssessmentScheduleProctorRepository springDataAssessmentScheduleProctorRepository) {
        this.springDataAssessmentScheduleProctorRepository = springDataAssessmentScheduleProctorRepository;
    }

    @Override
    public AssessmentScheduleProctor save(AssessmentScheduleProctor proctor) {
        try {
            var saved = springDataAssessmentScheduleProctorRepository.save(AssessmentScheduleProctorMapper.toJpa(proctor));
            return AssessmentScheduleProctorMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceDuplicatedException("Giám thị đã được phân công cho ca này");
        }
    }

    @Override
    public Optional<AssessmentScheduleProctor> findById(UUID id) {
        return springDataAssessmentScheduleProctorRepository.findById(id)
            .map(AssessmentScheduleProctorMapper::toDomain);
    }

    @Override
    public List<AssessmentScheduleProctor> findByScheduleId(UUID scheduleId) {
        return springDataAssessmentScheduleProctorRepository.findByScheduleId(scheduleId).stream()
            .map(AssessmentScheduleProctorMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId) {
        return springDataAssessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, teacherId);
    }

    @Override
    public long countByScheduleId(UUID scheduleId) {
        return springDataAssessmentScheduleProctorRepository.countByScheduleId(scheduleId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssessmentScheduleProctorRepository.deleteById(id);
    }

    @Override
    public void deleteByScheduleIdIn(java.util.Collection<UUID> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return;
        }
        springDataAssessmentScheduleProctorRepository.deleteByScheduleIdIn(scheduleIds);
    }

    @Override
    public List<UUID> findScheduleIdsByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentScheduleProctorRepository.findByTeacherIdAndScheduleIdIn(teacherId, scheduleIds)
            .stream()
            .map(e -> e.getScheduleId())
            .distinct()
            .toList();
    }

    @Override
    public boolean existsOverlappingAssignment(UUID teacherId, Instant start, Instant end, UUID excludeScheduleId) {
        // Ca thi chưa đặt giờ thì không có gì để so — coi như không vướng, đúng như cách
        // AssessmentScheduleRoomValidator bỏ qua khi thiếu dữ liệu.
        if (teacherId == null || start == null || end == null) {
            return false;
        }
        return springDataAssessmentScheduleProctorRepository
            .countOverlappingAssignments(teacherId, start, end, excludeScheduleId) > 0;
    }

    @Override
    public List<ProctorScheduleConflict> findConflictsForTeachers(
            Collection<UUID> teacherIds, Instant start, Instant end, UUID excludeScheduleId) {
        if (teacherIds == null || teacherIds.isEmpty() || start == null || end == null) {
            return List.of();
        }
        return springDataAssessmentScheduleProctorRepository
            .findOverlappingAssignments(teacherIds, start, end, excludeScheduleId).stream()
            .map(row -> new ProctorScheduleConflict(
                (UUID) row[0], (UUID) row[1], (Instant) row[2], (Instant) row[3]))
            .toList();
    }
}
