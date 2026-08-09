package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
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
    public List<ExamSchedule> findByExamIdIn(Collection<UUID> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return List.of();
        }
        return springDataExamScheduleRepository.findByExamIdInAndStatusNot(examIds, ExamScheduleStatus.DELETED.name()).stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSchedule> findBySchoolId(UUID schoolId) {
        return springDataExamScheduleRepository.findBySchoolId(schoolId).stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    /**
     * Loại ca đã xoá mềm giống findByExamId/findBySchoolId -- nếu không, các màn dùng batch load
     * theo id (lịch thi của học sinh, của giáo viên, DataLoader scheduleById) vẫn thấy ca đã xoá.
     */
    @Override
    public List<ExamSchedule> findByIdIn(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataExamScheduleRepository.findByIdInAndStatusNot(ids, ExamScheduleStatus.DELETED.name()).stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    /**
     * Ngược lại với findByIdIn/findByExamId: KHÔNG lọc DELETED. Dọn dữ liệu con lúc xoá hẳn kỳ thi
     * phải chạm cả ca đã xoá mềm, nếu không giám thị của những ca đó ở lại làm dòng mồ côi.
     */
    @Override
    public List<UUID> findAllIdsByExamId(UUID examId) {
        return springDataExamScheduleRepository.findAllIdsByExamId(examId);
    }

    @Override
    public void deleteByExamId(UUID examId) {
        springDataExamScheduleRepository.deleteByExamId(examId);
    }

    @Override
    public ExamSchedule save(ExamSchedule schedule) {
        var saved = springDataExamScheduleRepository.save(ExamScheduleMapper.toJpa(schedule));
        return ExamScheduleMapper.toDomain(saved);
    }

    @Override
    public boolean existsOverlapping(UUID schoolRoomId, Instant start, Instant end, UUID excludeScheduleId) {
        return springDataExamScheduleRepository.countOverlapping(schoolRoomId, start, end, excludeScheduleId) > 0;
    }

    @Override
    public int updateAtomic(UUID id, UUID schoolRoomId, Instant start, Instant end,
            Instant now, UUID updatedBy) {
        return springDataExamScheduleRepository.updateAtomic(id, schoolRoomId, start, end, now, updatedBy);
    }
    
    @Override
    public List<ExamSchedule> findPublishedEndedBefore(Instant now, int limit) {
        return springDataExamScheduleRepository
            .findPublishedEndedBefore(now, org.springframework.data.domain.PageRequest.of(0, limit))
            .stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSchedule> findByExamIdAndInSchedule(UUID examId, Instant now) {
        return springDataExamScheduleRepository.findByExamIdAndInSchedule(examId, now)
            .stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamSchedule> findByIdInAndInSchedule(Collection<UUID> ids, Instant now) {
        return springDataExamScheduleRepository.findByIdInAndInSchedule(ids, now)
            .stream()
            .map(ExamScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<ExamSchedule> findByIdAndInSchedule(UUID id, Instant now) {
        return springDataExamScheduleRepository.findByIdAndInSchedule(id, now)
            .map(ExamScheduleMapper::toDomain);
    }

    @Override
    public List<ExamSchedule> findByIdInAndInScheduleAndSchoolId(Collection<UUID> ids, Instant now,
            UUID schoolId) {
        return springDataExamScheduleRepository.findByIdInAndInScheduleAndSchoolId(ids, now, schoolId)
                .stream()
                .map(ExamScheduleMapper::toDomain)
                .toList();
    }
    
}
