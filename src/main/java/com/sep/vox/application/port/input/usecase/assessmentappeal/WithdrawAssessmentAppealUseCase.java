package com.sep.vox.application.port.input.usecase.assessmentappeal;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

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
@RequiredArgsConstructor
public class WithdrawAssessmentAppealUseCase implements IUseCase<UUID, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    @Override
    @Transactional
    public UUID execute(UUID appealId) {
        UUID currentUserId = assessmentAppealAccessService.requireActiveUserId();
        AssessmentAppealAccessService.AppealContext context = assessmentAppealAccessService.load(appealId);
        assessmentAppealAccessService.authorizeOwningStudent(context, currentUserId);

        AssessmentResultAppeal appeal = context.appeal();
        if (appeal.getStatus() != AssessmentAppealStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể rút đơn khi đơn còn đang chờ duyệt.");
        }

        Instant now = Instant.now();
        appeal.setStatus(AssessmentAppealStatus.WITHDRAWN);
        appeal.setWithdrawnAt(now);
        appeal.setResolvedAt(now);
        appeal.setResolvedBy(currentUserId);
        assessmentResultAppealRepository.save(appeal);
        releaseResultIfStillAppealed(context.candidateResult(), currentUserId);

        return appeal.getId();
    }

    /**
     * Returns the result to RELEASED only while it really sits in an appeal status. A result that moved on (FINAL,
     * PASSED/FAILED, INVALID...) must not be pulled back by a withdrawal, otherwise one student action would
     * reopen a closed result.
     */
    private void releaseResultIfStillAppealed(AssessmentCandidateResult candidateResult, UUID currentUserId) {
        AssessmentCandidateResultStatus status = candidateResult.getStatus();
        if (status == AssessmentCandidateResultStatus.APPEALED || status == AssessmentCandidateResultStatus.RE_GRADING) {
            ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(candidateResult);
            candidateResult.setStatus(AssessmentCandidateResultStatus.RELEASED);
            assessmentCandidateResultRepository.save(candidateResult);
            resultStatusHistoryRecorder.record(
                before, candidateResult, ResultStatusChangeSource.SYSTEM, currentUserId,
                "Học sinh rút đơn phúc khảo.");
        }
    }
}
