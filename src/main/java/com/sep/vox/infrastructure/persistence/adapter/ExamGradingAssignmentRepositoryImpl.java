package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamGradingAssignmentMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamGradingAssignmentRepository;

@Repository
public class ExamGradingAssignmentRepositoryImpl implements ExamGradingAssignmentRepository {

    private final SpringDataExamGradingAssignmentRepository springDataExamGradingAssignmentRepository;

    public ExamGradingAssignmentRepositoryImpl(
            SpringDataExamGradingAssignmentRepository springDataExamGradingAssignmentRepository) {
        this.springDataExamGradingAssignmentRepository = springDataExamGradingAssignmentRepository;
    }

    @Override
    public Optional<ExamGradingAssignment> findById(UUID id) {
        return springDataExamGradingAssignmentRepository.findById(id)
            .map(ExamGradingAssignmentMapper::toDomain);
    }

    @Override
    public Optional<ExamGradingAssignment> findOpenByCandidateResultId(UUID candidateResultId) {
        return springDataExamGradingAssignmentRepository.findByActiveResultId(candidateResultId)
            .map(ExamGradingAssignmentMapper::toDomain);
    }

    @Override
    public List<ExamGradingAssignment> findOpenByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataExamGradingAssignmentRepository.findByActiveResultIdIn(candidateResultIds).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId) {
        return springDataExamGradingAssignmentRepository
            .findByCandidateResultIdOrderByAssignedAtDesc(candidateResultId).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataExamGradingAssignmentRepository.findByCandidateResultIdIn(candidateResultIds).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findByAppealId(UUID appealId) {
        return springDataExamGradingAssignmentRepository.findByAppealId(appealId).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findOverdue(OffsetDateTime now) {
        return springDataExamGradingAssignmentRepository.findOverdue(now).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamGradingAssignment> findDueForReminder(OffsetDateTime threshold) {
        return springDataExamGradingAssignmentRepository.findDueForReminder(threshold).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    /**
     * Flush ngay, không đợi cuối transaction. Hai lý do, cả hai đều là lỗi thật nếu bỏ:
     *
     * <ol>
     *   <li>Unique index trên {@code active_result_id} nhạy với THỨ TỰ ghi. Hibernate
     *       mặc định flush INSERT trước UPDATE, nên "đóng vòng cũ rồi mở vòng mới cho
     *       cùng một bài" (chính là {@code CLEAR_INVALID}) sẽ chèn dòng mới trước khi
     *       nhả {@code active_result_id} của dòng cũ -> vi phạm unique.
     *   <li>{@code @Version} chỉ báo xung đột lúc flush. Flush sớm cho lỗi nổ ra đúng
     *       chỗ gọi, thay vì nổ ở commit khi đã không còn ngữ cảnh để dịch sang tiếng Việt.
     * </ol>
     */
    @Override
    public ExamGradingAssignment save(ExamGradingAssignment assignment) {
        var saved = springDataExamGradingAssignmentRepository.saveAndFlush(
            ExamGradingAssignmentMapper.toJpa(assignment));
        return ExamGradingAssignmentMapper.toDomain(saved);
    }

    @Override
    public List<ExamGradingAssignment> saveAll(List<ExamGradingAssignment> assignments) {
        var entities = assignments.stream().map(ExamGradingAssignmentMapper::toJpa).toList();
        return springDataExamGradingAssignmentRepository.saveAllAndFlush(entities).stream()
            .map(ExamGradingAssignmentMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataExamGradingAssignmentRepository.deleteById(id);
    }

    @Override
    public void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return;
        }
        springDataExamGradingAssignmentRepository.deleteByCandidateResultIdIn(candidateResultIds);
    }
}
