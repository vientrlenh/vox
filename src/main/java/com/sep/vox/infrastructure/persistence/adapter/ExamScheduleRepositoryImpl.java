package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
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
    public Optional<ExamSchedule> findByIdForUpdate(UUID id) {
        return springDataExamScheduleRepository.findByIdForUpdate(id)
            .map(ExamScheduleMapper::toDomain);
    }

    @Override
    public List<ExamSchedule> findByExamId(UUID examId) {
        return springDataExamScheduleRepository.findByExamIdAndStatusNot(examId, ExamScheduleStatus.DELETED.name()).stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSchedule> findBySchoolId(UUID schoolId) {
        return springDataExamScheduleRepository.findBySchoolId(schoolId).stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSchedule> findByIdIn(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataExamScheduleRepository.findByIdIn(ids).stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public ExamSchedule save(ExamSchedule schedule) {
        var saved = springDataExamScheduleRepository.save(ExamScheduleMapper.toJpa(schedule));
        return ExamScheduleMapper.toDomain(saved);
    }

    @Override
    public boolean existsOverlapping(UUID schoolRoomId, OffsetDateTime start, OffsetDateTime end, UUID excludeScheduleId) {
        return springDataExamScheduleRepository.countOverlapping(schoolRoomId, start, end, excludeScheduleId) > 0;
    }

    @Override
    public int updateAtomic(UUID id, UUID schoolRoomId, OffsetDateTime start, OffsetDateTime end,
            OffsetDateTime now, UUID updatedBy) {
        return springDataExamScheduleRepository.updateAtomic(id, schoolRoomId, start, end, now, updatedBy);
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

    @Override
    public Optional<ExamSchedule> findByIdAndInSchedule(UUID id, OffsetDateTime now) {
        return springDataExamScheduleRepository.findByIdAndInSchedule(id, now)
            .map(ExamScheduleMapper::toDomain);
    }

    @Override
    public List<ExamSchedule> findByIdInAndInScheduleAndSchoolId(Collection<UUID> ids, OffsetDateTime now,
            UUID schoolId) {
        return springDataExamScheduleRepository.findByIdInAndInScheduleAndSchoolId(ids, now, schoolId)
                .stream()
                .map(ExamScheduleMapper::toDomain)
                .toList();
    }
    
}
