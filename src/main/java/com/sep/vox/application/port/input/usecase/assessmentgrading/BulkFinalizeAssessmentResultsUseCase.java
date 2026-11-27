package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentgrading.BulkFinalizeAssessmentResultsCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Close the books: moves the attempts of an assessment that are still waiting for a grader ({@code PENDING_REVIEW}) to
 * {@code RELEASED} using the AI score they have, so the assessment can be published.
 *
 * <p>This is the way out of the situation where exactly ONE ungraded attempt blocks the whole assessment from
 * being published (review BE-5): {@code finalizeForPublish} skips
 * {@code PENDING_REVIEW} attempts, so previously the assessment was stuck indefinitely and no button could unblock it.
 *
 * <p><strong>This use case does NOT produce {@code FINAL}.</strong> {@code FINAL} is a
 * <em>post</em>-publish status -- {@code finalizeForPublish} sets it when the policy has no
 * {@code passingScore} and the school has to decide pass/fail itself. Producing it here would make
 * {@code requirePublishReadiness} (which only accepts {@code RELEASED}/{@code INVALID}) reject the whole
 * assessment, while the only way out of {@code FINAL} --
 * {@code DecideAssessmentCandidateResultOutcomeUseCase} -- requires the assessment to be already published: a closed
 * deadlock that can only be removed with manual SQL. Closing the books therefore only moves attempts into the two states that
 * publish accepts, and lets publish take care of the final judgment.
 *
 * <p>Three layers of protection so it does not become a "publish blindly" button:
 * <ol>
 *   <li>A pending score dispute is a HARD blocking condition that no flag can skip:
 *       an open appeal, and an attempt in {@code APPEALED}/{@code RE_GRADING} -- in
 *       {@code RE_GRADING} there is also a teacher holding a half-finished grading.
 *   <li>A preview is mandatory -- if blocking attempts remain and the admin has not confirmed
 *       {@code releasePendingWithAiScores}, the use case refuses and says exactly how many attempts remain.
 *   <li>Every status change goes into the log with the source {@code ADMIN_BULK_FINALIZE},
 *       so it can later be traced which attempts were published on the AI score and not on a human score.
 * </ol>
 *
 * <p>{@code RELEASED} attempts are already ready to publish so they are left untouched. {@code INVALID} attempts
 * are kept as they are: they already have a conclusion, and {@code finalizeForPublish} will move them to
 * {@code FAILED} on its own (score forced to 0) when the assessment is published -- pulling them through any other
 * status would open the way for a violating attempt to come out with a {@code PASSED} result.
 */
@Service
@RequiredArgsConstructor
public class BulkFinalizeAssessmentResultsUseCase
        implements IUseCase<BulkFinalizeAssessmentResultsCommand, Integer> {

    private static final String BULK_REASON = "Chốt sổ hàng loạt theo kỳ thi.";
    private static final String BULK_AI_REASON =
        "Chốt sổ hàng loạt: công bố theo điểm AI vì chưa có người chấm.";

    /**
     * Pending score dispute: blocks the whole finalize run, and no flag can skip it.
     *
     * <p>With consistent data, {@code preview.openAppeals()} already blocks first (a result is
     * in these two statuses only while an appeal is open). The check here is a safety net for data that
     * is inconsistent the other way round: an appeal is closed but the result was not moved back to
     * {@code RELEASED}, which used to be hidden by {@code FINAL} instead of being exposed.
     */
    private static final Set<AssessmentCandidateResultStatus> BLOCKING_STATUSES = EnumSet.of(
        AssessmentCandidateResultStatus.APPEALED,
        AssessmentCandidateResultStatus.RE_GRADING);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;

    @Override
    @Transactional
    public Integer execute(BulkFinalizeAssessmentResultsCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        Assessment assessment = requireAuthorizedAssessment(command.assessmentId(), currentUserId);

        BulkFinalizePreviewInfo preview = assessmentGradingQueryRepository.previewBulkFinalize(assessment.getSchoolId(), command.assessmentId());
        requireNoOpenAppeals(preview);

        List<AssessmentCandidateResult> results = assessmentCandidateResultRepository.findByAssessmentId(command.assessmentId());
        requireReadyToFinalize(results, preview, command.releasePendingWithAiScores());

        Instant now = Instant.now();
        ReleaseOutcome outcome = releasePendingResults(results, currentUserId, now);

        closeSettledAssignments(outcome.released(), now);
        resultStatusHistoryRecorder.recordAll(List.copyOf(outcome.histories()));
        return outcome.released().size();
    }

    private Assessment requireAuthorizedAssessment(UUID assessmentId, UUID currentUserId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
        // A class test: the school does not grade it, so if only the school admin could close the
        // books the teacher would be stuck -- publishing results requires EVERY attempt to be RELEASED
        // or INVALID (UpdateAssessmentStatusUseCase.requirePublishReadiness).
        assessmentGradingAccessService.authorizeSchoolAdminOrClassTestChair(
            assessment.getSchoolId(), assessment.getId(), currentUserId);
        return assessment;
    }

    /**
     * An open appeal ALWAYS blocks, even when the admin has ticked "publish with the existing
     * scores": that flag only says "accept the AI score for attempts nobody has graded", not
     * "swallow the students' score disputes as well". Swallowing an appeal also leaves two
     * consequences: the appeal hangs open forever, and a student who later withdraws it would pull
     * the attempt back to RELEASED even though the assessment has already been published.
     */
    private void requireNoOpenAppeals(BulkFinalizePreviewInfo preview) {
        if (preview.openAppeals() > 0) {
            throw new IllegalArgumentException(
                "Kỳ thi còn " + preview.openAppeals() + " đơn phúc khảo chưa xong. "
                    + "Hãy xử lý hoặc từ chối các đơn này trước khi chốt sổ.");
        }
    }

    private void requireReadyToFinalize(
            List<AssessmentCandidateResult> results, BulkFinalizePreviewInfo preview, boolean releasePendingWithAiScores) {
        Map<AssessmentCandidateResultStatus, Integer> blocking = countBlocking(results);
        if (!blocking.isEmpty()) {
            throw new IllegalArgumentException(describeBlocking(blocking));
        }
        if (!preview.isClean() && !releasePendingWithAiScores) {
            throw new IllegalArgumentException(
                "Kỳ thi còn " + preview.pendingUnassigned() + " bài chưa ai chấm và "
                    + preview.pendingAssigned() + " bài đang chấm dở. "
                    + "Hãy xử lý nốt, hoặc xác nhận công bố theo điểm AI hiện có.");
        }
    }

    private ReleaseOutcome releasePendingResults(
            List<AssessmentCandidateResult> results, UUID currentUserId, Instant now) {
        List<AssessmentResultStatusHistory> histories = new ArrayList<AssessmentResultStatusHistory>();
        List<AssessmentCandidateResult> released = new ArrayList<AssessmentCandidateResult>();

        for (AssessmentCandidateResult result : results) {
            if (result.getStatus() != AssessmentCandidateResultStatus.PENDING_REVIEW) {
                continue;
            }
            release(result, now);
            released.add(result);
            histories.add(releasedHistory(result, currentUserId, now));
        }
        return new ReleaseOutcome(released, histories);
    }

    private void release(AssessmentCandidateResult result, Instant now) {
        result.setStatus(AssessmentCandidateResultStatus.RELEASED);
        // An attempt that was RELEASED once and then pulled back to PENDING_REVIEW (invalidation lifted)
        // keeps its old release timestamp: the student's appeal window is counted from that timestamp.
        if (result.getReleasedAt() == null) {
            result.setReleasedAt(now);
        }
        assessmentCandidateResultRepository.save(result);
    }

    private AssessmentResultStatusHistory releasedHistory(
            AssessmentCandidateResult result, UUID currentUserId, Instant now) {
        return AssessmentResultStatusHistory.transition(
            result.getId(),
            AssessmentCandidateResultStatus.PENDING_REVIEW,
            AssessmentCandidateResultStatus.RELEASED,
            result.getTotalScore(),
            result.getTotalScore(),
            ResultStatusChangeSource.ADMIN_BULK_FINALIZE,
            currentUserId,
            BULK_AI_REASON,
            now);
    }

    /**
     * Đóng phân công còn mở trên các bài VỪA được công bố. Bài đã rời
     * {@code PENDING_REVIEW} nên vòng {@code INITIAL} không còn xử lý được nó nữa
     * ({@code GradingRoundPolicy.isAssignable} trả false); không đóng thì phân công treo
     * vĩnh viễn trong hàng đợi của giáo viên.
     *
     * <p>Chỉ đụng đúng những bài use case này chuyển trạng thái. Vòng {@code SPOT_CHECK}
     * trên bài {@code RELEASED} và vòng {@code REMEDIATION} trên bài {@code INVALID} vẫn
     * còn nguyên việc để làm — đóng chúng là xoá một quyết định chưa được đưa ra.
     */
    private void closeSettledAssignments(List<AssessmentCandidateResult> released, Instant now) {
        if (released.isEmpty()) {
            return;
        }
        List<AssessmentGradingAssignment> openAssignments = assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(
            released.stream().map(result -> result.getId()).toList());
        for (AssessmentGradingAssignment assignment : openAssignments) {
            assignment.complete(GradingOutcome.DECLINED, BULK_REASON, now);
            assessmentGradingAssignmentRepository.save(assignment);
        }
    }

    /** {@code EnumMap} để thứ tự kể trong thông báo bám theo thứ tự khai báo của enum. */
    private Map<AssessmentCandidateResultStatus, Integer> countBlocking(List<AssessmentCandidateResult> results) {
        Map<AssessmentCandidateResultStatus, Integer> counts = new EnumMap<AssessmentCandidateResultStatus, Integer>(AssessmentCandidateResultStatus.class);
        for (AssessmentCandidateResult result : results) {
            AssessmentCandidateResultStatus status = result.getStatus();
            if (BLOCKING_STATUSES.contains(status)) {
                Integer current = counts.get(status);
                counts.put(status, current == null ? 1 : current + 1);
            }
        }
        return counts;
    }

    /**
     * Kể HẾT mọi thứ đang chặn trong một thông báo: báo từng thứ một là bắt admin bấm lại
     * nhiều lần mới biết còn gì phải xử lý.
     */
    private String describeBlocking(Map<AssessmentCandidateResultStatus, Integer> counts) {
        List<String> parts = new ArrayList<String>();
        counts.forEach((status, count) -> parts.add(switch (status) {
            case APPEALED -> count + " bài có đơn phúc khảo chờ xử lý";
            case RE_GRADING -> count + " bài đang được chấm phúc khảo";
            default -> count + " bài ở trạng thái " + status.name();
        }));
        return "Kỳ thi còn " + String.join(", ", parts)
            + ". Chốt sổ không quyết thay tranh chấp điểm — hãy xử lý dứt điểm các bài này trước.";
    }

    private record ReleaseOutcome(
        List<AssessmentCandidateResult> released,
        List<AssessmentResultStatusHistory> histories
    ) {
    }
}
