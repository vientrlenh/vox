package com.sep.vox.application.port.input.usecase.assessmentapeal;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusChangeSource;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;

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
public class WithdrawAssessmentAppealUseCase implements IUseCase<UUID, UUID> {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentAppealAccessService assessmentAppealAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    public WithdrawAssessmentAppealUseCase(
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentAppealAccessService assessmentAppealAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder) {
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentAppealAccessService = assessmentAppealAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
    }

    @Override
    @Transactional
    public UUID execute(UUID appealId) {
        var currentUserId = assessmentAppealAccessService.requireActiveUserId();
        var context = assessmentAppealAccessService.load(appealId);
        assessmentAppealAccessService.authorizeOwningStudent(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != AssessmentAppealStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể rút đơn khi đơn còn đang chờ duyệt.");
        }

        var now = Instant.now();
        appeal.setStatus(AssessmentAppealStatus.WITHDRAWN);
        appeal.setWithdrawnAt(now);
        appeal.setResolvedAt(now);
        appeal.setResolvedBy(currentUserId);
        assessmentResultAppealRepository.save(appeal);

        // Chỉ trả bài về RELEASED khi nó đang thật sự đứng ở trạng thái phúc khảo.
        // Bài đã đi tiếp (FINAL, PASSED/FAILED, INVALID...) thì rút đơn không được
        // phép kéo ngược trạng thái — nếu không, một thao tác của học sinh mở lại
        // được kết quả đã chốt sổ.
        var candidateResult = context.candidateResult();
        var status = candidateResult.getStatus();
        if (status == AssessmentCandidateResultStatus.APPEALED || status == AssessmentCandidateResultStatus.RE_GRADING) {
            var before = resultStatusHistoryRecorder.snapshot(candidateResult);
            candidateResult.setStatus(AssessmentCandidateResultStatus.RELEASED);
            candidateResult.setUpdatedAt(now);
            candidateResult.setUpdatedBy(currentUserId);
            assessmentCandidateResultRepository.save(candidateResult);
            resultStatusHistoryRecorder.record(
                before, candidateResult, AssessmentResultStatusChangeSource.SYSTEM, currentUserId,
                "Học sinh rút đơn phúc khảo.");
        }

        return appeal.getId();
    }
}
