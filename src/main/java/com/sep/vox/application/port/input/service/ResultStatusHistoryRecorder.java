package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamResultStatusHistory;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;

/**
 * Chỗ ghi audit DUY NHẤT cho mọi lần đổi trạng thái / đổi điểm của một bài.
 *
 * <p>Có hai chỗ ghi là mất audit: tranh chấp điểm sẽ rơi vào đúng cái chỗ không ghi.
 * Mọi use case chạm {@link ExamCandidateResult#setStatus} hoặc làm tổng điểm thay đổi
 * đều phải đi qua đây.
 *
 * <p>Cách dùng: chụp {@link #snapshot(ExamCandidateResult)} <em>trước</em> khi sửa,
 * gọi {@link #record} sau khi đã có kết quả mới.
 */
@Service
public class ResultStatusHistoryRecorder {

    private final ExamResultStatusHistoryRepository examResultStatusHistoryRepository;

    public ResultStatusHistoryRecorder(ExamResultStatusHistoryRepository examResultStatusHistoryRepository) {
        this.examResultStatusHistoryRepository = examResultStatusHistoryRepository;
    }

    /** Ảnh chụp trạng thái + điểm trước khi thao tác. */
    public record Snapshot(UUID candidateResultId, ExamCandidateResultStatus status, BigDecimal totalScore) {
    }

    public Snapshot snapshot(ExamCandidateResult result) {
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
    public ExamResultStatusHistory record(Snapshot before, ExamCandidateResult after,
            ResultStatusChangeSource source, UUID actorId, String reason) {
        if (before == null || after == null) {
            return null;
        }
        var statusChanged = before.status() != after.getStatus();
        var scoreChanged = !equalScores(before.totalScore(), after.getTotalScore());
        if (!statusChanged && !scoreChanged) {
            return null;
        }
        return examResultStatusHistoryRepository.save(new ExamResultStatusHistory(
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
    public ExamResultStatusHistory recordAlways(Snapshot before, ExamCandidateResult after,
            ResultStatusChangeSource source, UUID actorId, String reason) {
        if (before == null || after == null) {
            return null;
        }
        return examResultStatusHistoryRepository.save(new ExamResultStatusHistory(
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
    public List<ExamResultStatusHistory> recordAll(List<ExamResultStatusHistory> histories) {
        return examResultStatusHistoryRepository.saveAll(histories);
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
