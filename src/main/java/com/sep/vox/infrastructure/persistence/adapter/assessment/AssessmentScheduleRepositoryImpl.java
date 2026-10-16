package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentScheduleMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentScheduleRepository;

@Repository
public class AssessmentScheduleRepositoryImpl implements AssessmentScheduleRepository {

    private final SpringDataAssessmentScheduleRepository springDataAssessmentScheduleRepository;

    public AssessmentScheduleRepositoryImpl(SpringDataAssessmentScheduleRepository springDataAssessmentScheduleRepository) {
        this.springDataAssessmentScheduleRepository = springDataAssessmentScheduleRepository;
    }

    @Override
    public Optional<AssessmentSchedule> findById(UUID id) {
        return springDataAssessmentScheduleRepository.findById(id)
            .map(AssessmentScheduleMapper::toDomain);
    }

    @Override
    public Optional<AssessmentSchedule> findByIdForUpdate(UUID id) {
        return springDataAssessmentScheduleRepository.findByIdForUpdate(id)
            .map(AssessmentScheduleMapper::toDomain);
    }

    @Override
    public List<AssessmentSchedule> findByAssessmentId(UUID assessmentId) {
        return springDataAssessmentScheduleRepository.findByAssessmentIdAndStatusNot(assessmentId, AssessmentScheduleStatus.DELETED.name()).stream()
            .map(AssessmentScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentSchedule> findByAssessmentIdIn(Collection<UUID> assessmentIds) {
        if (assessmentIds == null || assessmentIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentScheduleRepository.findByAssessmentIdInAndStatusNot(assessmentIds, AssessmentScheduleStatus.DELETED.name()).stream()
            .map(AssessmentScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentSchedule> findBySchoolId(UUID schoolId) {
        return springDataAssessmentScheduleRepository.findBySchoolId(schoolId).stream()
            .map(AssessmentScheduleMapper::toDomain)
            .toList();
    }

    /**
     * Loại ca đã xoá mềm giống findByAssessmentId/findBySchoolId -- nếu không, các màn dùng batch load
     * theo id (lịch thi của học sinh, của giáo viên, DataLoader scheduleById) vẫn thấy ca đã xoá.
     */
    @Override
    public List<AssessmentSchedule> findByIdIn(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentScheduleRepository.findByIdInAndStatusNot(ids, AssessmentScheduleStatus.DELETED.name()).stream()
            .map(AssessmentScheduleMapper::toDomain)
            .toList();
    }

    /**
     * Ngược lại với findByIdIn/findByAssessmentId: KHÔNG lọc DELETED. Dọn dữ liệu con lúc xoá hẳn kỳ thi
     * phải chạm cả ca đã xoá mềm, nếu không giám thị của những ca đó ở lại làm dòng mồ côi.
     */
    @Override
    public List<UUID> findAllIdsByAssessmentId(UUID assessmentId) {
        return springDataAssessmentScheduleRepository.findAllIdsByAssessmentId(assessmentId);
    }

    @Override
    public void deleteByAssessmentId(UUID assessmentId) {
        springDataAssessmentScheduleRepository.deleteByAssessmentId(assessmentId);
    }

    @Override
    public AssessmentSchedule save(AssessmentSchedule schedule) {
        var saved = springDataAssessmentScheduleRepository.save(AssessmentScheduleMapper.toJpa(schedule));
        return AssessmentScheduleMapper.toDomain(saved);
    }

    @Override
    public boolean existsOverlapping(UUID schoolRoomId, Instant start, Instant end, UUID excludeScheduleId) {
        return springDataAssessmentScheduleRepository.countOverlapping(schoolRoomId, start, end, excludeScheduleId) > 0;
    }

    @Override
    public int updateAtomic(UUID id, UUID schoolRoomId, Instant start, Instant end,
            Instant now, UUID updatedBy) {
        return springDataAssessmentScheduleRepository.updateAtomic(id, schoolRoomId, start, end, now, updatedBy);
    }
    
    @Override
    public List<AssessmentSchedule> findPublishedEndedBefore(Instant now, int limit) {
        return springDataAssessmentScheduleRepository
            .findPublishedEndedBefore(now, org.springframework.data.domain.PageRequest.of(0, limit))
            .stream()
            .map(AssessmentScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentSchedule> findByAssessmentIdAndInSchedule(UUID assessmentId, Instant now) {
        return springDataAssessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessmentId, now)
            .stream()
            .map(AssessmentScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentSchedule> findByIdInAndInSchedule(Collection<UUID> ids, Instant now) {
        return springDataAssessmentScheduleRepository.findByIdInAndInSchedule(ids, now)
            .stream()
            .map(AssessmentScheduleMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentSchedule> findByIdAndInSchedule(UUID id, Instant now) {
        return springDataAssessmentScheduleRepository.findByIdAndInSchedule(id, now)
            .map(AssessmentScheduleMapper::toDomain);
    }

    @Override
    public List<AssessmentSchedule> findByIdInAndInScheduleAndSchoolId(Collection<UUID> ids, Instant now,
            UUID schoolId) {
        return springDataAssessmentScheduleRepository.findByIdInAndInScheduleAndSchoolId(ids, now, schoolId)
                .stream()
                .map(AssessmentScheduleMapper::toDomain)
                .toList();
    }
    
}
