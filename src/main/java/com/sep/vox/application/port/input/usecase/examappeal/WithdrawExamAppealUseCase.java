package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * Học sinh rút đơn phúc khảo khi đơn còn {@code PENDING}.
 *
 * <p>Bài quay về {@code RELEASED} và <em>lượt phúc khảo được hoàn lại</em>: hạn mức
 * đếm số đơn đã {@code PUBLISHED}, mà đơn rút thì chưa ai chấm nên không đốt lượt.
 *
 * <p>Chỉ cho rút ở {@code PENDING}. Từ {@code APPROVED} trở đi đã có người bị điều
 * động (và có thể đã đọc bài), rút lúc đó là lãng phí công của trường — muốn dừng thì
 * đi đường admin từ chối, có ghi lý do.
 */
@Service
public class WithdrawExamAppealUseCase implements IUseCase<UUID, UUID> {

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamAppealAccessService examAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    public WithdrawExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamAppealAccessService examAppealAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examAppealAccessService = examAppealAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
    }

    @Override
    @Transactional
    public UUID execute(UUID appealId) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(appealId);
        examAppealAccessService.authorizeOwningStudent(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể rút đơn khi đơn còn đang chờ duyệt.");
        }

        var now = Instant.now();
        appeal.setStatus(ExamAppealStatus.WITHDRAWN);
        appeal.setWithdrawnAt(now);
        appeal.setResolvedAt(now);
        appeal.setResolvedBy(currentUserId);
        examResultAppealRepository.save(appeal);

        // Chỉ trả bài về RELEASED khi nó đang thật sự đứng ở trạng thái phúc khảo.
        // Bài đã đi tiếp (FINAL, PASSED/FAILED, INVALID...) thì rút đơn không được
        // phép kéo ngược trạng thái — nếu không, một thao tác của học sinh mở lại
        // được kết quả đã chốt sổ.
        var candidateResult = context.candidateResult();
        var status = candidateResult.getStatus();
        if (status == ExamCandidateResultStatus.APPEALED || status == ExamCandidateResultStatus.RE_GRADING) {
            var before = resultStatusHistoryRecorder.snapshot(candidateResult);
            candidateResult.setStatus(ExamCandidateResultStatus.RELEASED);
            candidateResult.setUpdatedAt(now);
            candidateResult.setUpdatedBy(currentUserId);
            examCandidateResultRepository.save(candidateResult);
            resultStatusHistoryRecorder.record(
                before, candidateResult, ResultStatusChangeSource.SYSTEM, currentUserId,
                "Học sinh rút đơn phúc khảo.");
        }

        return appeal.getId();
    }
}
