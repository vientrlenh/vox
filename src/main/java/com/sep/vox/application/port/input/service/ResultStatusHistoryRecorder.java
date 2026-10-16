package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusChangeSource;
import com.sep.vox.domain.repository.assessment.AssessmentResultStatusHistoryRepository;

/**
 * Chỗ ghi audit DUY NHẤT cho mọi lần đổi trạng thái / đổi điểm của một bài.
 *
 * <p>Có hai chỗ ghi là mất audit: tranh chấp điểm sẽ rơi vào đúng cái chỗ không ghi.
 * Mọi use case chạm {@link AssessmentCandidateResult#setStatus} hoặc làm tổng điểm thay đổi
 * đều phải đi qua đây.
 *
 * <p>Cách dùng: chụp {@link #snapshot(AssessmentCandidateResult)} <em>trước</em> khi sửa,
 * gọi {@link #record} sau khi đã có kết quả mới.
 */
@Service
public class ResultStatusHistoryRecorder {

    private final AssessmentResultStatusHistoryRepository assessmentResultStatusHistoryRepository;

    public ResultStatusHistoryRecorder(AssessmentResultStatusHistoryRepository assessmentResultStatusHistoryRepository) {
        this.assessmentResultStatusHistoryRepository = assessmentResultStatusHistoryRepository;
    }

    /** Ảnh chụp trạng thái + điểm trước khi thao tác. */
    public record Snapshot(UUID candidateResultId, AssessmentCandidateResultStatus status, BigDecimal totalScore) {
    }

    public Snapshot snapshot(AssessmentCandidateResult result) {
        if (result == null) {
            return null;
        }
        return new Snapshot(result.getId(), result.getStatus(), result.getTotalScore());
    }

    /**
     * Ghi một dòng nhật ký nếu có gì đó thực sự đổi (trạng thái hoặc điểm).
     *
     * <p>Trả về {@code null} khi không có gì đổi — hành động "giữ nguyên" ở vòng hậu
     * kiểm không sinh dòng rác, nhưng bản thân việc giáo viên đã xác nhận vẫn nằm ở
     * {@code outcome} của dòng phân công.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentResultStatusHistory record(Snapshot before, AssessmentCandidateResult after,
            AssessmentResultStatusChangeSource source, UUID actorId, String reason) {
        if (before == null || after == null) {
            return null;
        }
        var statusChanged = before.status() != after.getStatus();
        var scoreChanged = !equalScores(before.totalScore(), after.getTotalScore());
        if (!statusChanged && !scoreChanged) {
            return null;
        }
        return assessmentResultStatusHistoryRepository.save(new AssessmentResultStatusHistory(
            after.getId(),
            before.status(),
            after.getStatus(),
            before.totalScore(),
            after.getTotalScore(),
            source,
            actorId,
            reason,
            Instant.now()
        ));
    }

    /**
     * Ghi thẳng, kể cả khi không có gì đổi. Dùng cho những quyết định phải để lại dấu
     * vết dù trạng thái đứng yên — ví dụ REMEDIATION giữ nguyên INVALID (xác nhận vi
     * phạm) hay giáo viên trả lại phân công.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentResultStatusHistory recordAlways(Snapshot before, AssessmentCandidateResult after,
            AssessmentResultStatusChangeSource source, UUID actorId, String reason) {
        if (before == null || after == null) {
            return null;
        }
        return assessmentResultStatusHistoryRepository.save(new AssessmentResultStatusHistory(
            after.getId(),
            before.status(),
            after.getStatus(),
            before.totalScore(),
            after.getTotalScore(),
            source,
            actorId,
            reason,
            Instant.now()
        ));
    }

    /** Ghi hàng loạt — chốt sổ cả kỳ thi không được đẻ ra N insert lẻ. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<AssessmentResultStatusHistory> recordAll(List<AssessmentResultStatusHistory> histories) {
        return assessmentResultStatusHistoryRepository.saveAll(histories);
    }

    /**
     * So điểm theo GIÁ TRỊ, không theo {@code equals}: {@code BigDecimal("7.0")} và
     * {@code BigDecimal("7.00")} không equals nhau nhưng là cùng một điểm — dùng
     * equals ở đây sẽ đẻ ra dòng audit ma mỗi lần tính lại.
     */
    private boolean equalScores(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return Objects.equals(left, right);
        }
        return left.compareTo(right) == 0;
    }
}
