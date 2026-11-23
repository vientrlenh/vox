package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.event.AssessmentResultsPublishedEvent;
import com.sep.vox.application.port.input.command.UpdateAssessmentStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentStatusUseCase implements IUseCase<UpdateAssessmentStatusCommand, AssessmentDto> {

    /**
     * Cancellable while the results are not yet finalized. RESULTS_PUBLISHED cannot be withdrawn (students have already seen their scores),
     * and CANCELLED cannot be cancelled again.
     */
    private static final Set<AssessmentStatus> CANCELLABLE_FROM =
        EnumSet.of(AssessmentStatus.DRAFT, AssessmentStatus.SCHEDULED, AssessmentStatus.IN_PROGRESS, AssessmentStatus.CLOSED);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final AssessmentCandidateResultFinalizationService assessmentCandidateResultFinalizationService;
    private final ZeroScoreAssessmentResultService zeroScoreAssessmentResultService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;
    private final AssessmentScheduleClosureService assessmentScheduleClosureService;

    @Override
    @Transactional
    public AssessmentDto execute(UpdateAssessmentStatusCommand input) {
        UpdateAssessmentStatusCommand command = new UpdateAssessmentStatusCommand(
            input.assessmentId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(user -> user.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        Assessment assessment = assessmentRepository.findById(command.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeMutation(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        Instant now = Instant.now();
        switch (command.action()) {
            case "SCHEDULE" -> {
                // Trạng thái kiểm TRƯỚC hạn mức gói: bấm SCHEDULE trên bài không còn DRAFT mà báo lỗi
                // "vượt quá giới hạn gói" thì người dùng đi sửa nhầm chỗ.
                requireTransition(assessment, AssessmentStatus.DRAFT, AssessmentStatus.SCHEDULED);
                validatePlanLimits(assessment);
                if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
                    requireClassTestScheduleReady(assessment);
                } else {
                    requireCentralizedScheduleReadiness(assessment);
                }
            }
            case "START" -> {
                if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
                    requireClassTestCanStart(assessment);
                }
                requireTransition(assessment, AssessmentStatus.SCHEDULED, AssessmentStatus.IN_PROGRESS);
                if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
                    lockClassTestPapers(assessment.getId());
                }
            }
            case "CLOSE" -> {
                requireTransition(assessment, AssessmentStatus.IN_PROGRESS, AssessmentStatus.CLOSED);
                // Kiểm trạng thái trước rồi mới đụng ca thi, giống nhánh CANCEL bên dưới.
                assessmentScheduleClosureService.requireNoActiveSessionInOngoingSchedule(assessment.getId(), now);
                assessmentScheduleClosureService.closeSchedulesForAssessment(assessment.getId(), now);
                assessmentQuestionSecureLockService.releaseIfAutoAfterClose(assessment.getId());
                zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
                // Quét bù SAU khi đã bù kết quả điểm 0: bài trên lớp không có ai điều phối
                // chấm, nên đóng bài là chốt cuối để giáo viên chủ bài nhận hết phần còn lại.
                classTestGradingAssignmentService.ensureAssignmentsForAssessment(assessment.getId());
                // Sau quét bù: con số "còn bao nhiêu bài chờ chấm" chỉ đúng khi hàng đợi đã
                // đầy đủ. Áp dụng cho CẢ hai loại bài -- quét bù ở trên chỉ lo bài trên lớp.
                assessmentHumanGradingNotificationService.publishIfPendingReview(assessment, now);
            }
            case "PUBLISH_RESULTS" -> {
                zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
                requirePublishReadiness(assessment.getId());
                requireTransition(assessment, AssessmentStatus.CLOSED, AssessmentStatus.RESULTS_PUBLISHED);
                finalizePassFailForAssessment(assessment);
                eventPublisherPort.publish(new AssessmentResultsPublishedEvent(assessment.getId()));
            }
            case "CANCEL" -> {
                // Kiểm trạng thái trước rồi mới đụng ca thi: huỷ không hợp lệ thì không được ghi gì.
                requireTransition(assessment, CANCELLABLE_FROM, AssessmentStatus.CANCELLED);
                assessmentScheduleClosureService.cancelSchedulesForAssessment(assessment.getId());
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        return AssessmentDtoMapper.toDto(assessmentRepository.save(assessment));
    }

    private void authorizeMutation(
            UUID assessmentId,
            UUID assessmentSchoolId,
            AssessmentKind kind,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == AssessmentKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
                return;
            }
            // The council chair runs the whole centralized assessment workflow on the detail page, so they are also the
            // one who presses schedule / start / close / publish results. Deleting an assessment is still only for the school
            // administrator (see DeleteAssessmentUseCase) -- that is a destructive action and is not part of this workflow.
            if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
                return;
            }
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireTransition(Assessment assessment, AssessmentStatus from, AssessmentStatus to) {
        requireTransition(assessment, EnumSet.of(from), to);
    }

    /**
     * Bản nhiều trạng thái nguồn, hiện chỉ CANCEL dùng: huỷ được từ bất kỳ trạng thái nào chưa chốt,
     * nhưng kết quả đã công bố thì không rút lại được, và bài đã huỷ không huỷ thêm lần nữa.
     */
    private void requireTransition(Assessment assessment, Set<AssessmentStatus> allowedFrom, AssessmentStatus to) {
        if (!allowedFrom.contains(assessment.getStatus())) {
            throw new IllegalStateException("Trạng thái bài kiểm tra hiện tại không hợp lệ cho action này");
        }
        assessment.setStatus(to);
    }

    private void validatePlanLimits(Assessment assessment) {
        SchoolSubscription activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(assessment.getSchoolId())
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể lên lịch kỳ thi"));
        SubscriptionPlan plan = subscriptionPlanRepository.findById(activeSubscription.getSubscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói subscription"));

        if (assessment.getAssessmentTimeDurationSecond() != null && plan.getMaxTimePerAttemptMin() != null
                && assessment.getAssessmentTimeDurationSecond() > plan.getMaxTimePerAttemptMin() * 60) {
            throw new PlanLimitExceededException(
                "Thời lượng bài thi (" + assessment.getAssessmentTimeDurationSecond() + " giây) vượt quá giới hạn của gói \""
                    + plan.getName() + "\" (tối đa " + plan.getMaxTimePerAttemptMin() + " phút/lượt thi)"
            );
        }

        // Ước lượng worst-case: mọi thí sinh dùng hết toàn bộ thời lượng bài thi cho mỗi lượt làm bài,
        // soi trước hạn mức GRADING (và với bài trên lớp, cả CLASS_TEST + hạn mức cá nhân giáo viên) --
        // xem ClassTestTokenQuotaGuardService để rõ vì sao phải soi cả 3 chỗ.
        classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
    }

    /**
     * Bài kiểm tra trên lớp cũng thi trong phòng có giám khảo như kỳ thi tập trung, nên trước khi
     * công bố lịch phải đủ: ca thi có phòng, có giám khảo, và mọi học sinh đã được xếp ca + có đề.
     *
     * <p>Chọn lớp lúc tạo chỉ nạp học sinh vào danh sách dự thi; xếp ca là bước riêng, nên nếu
     * không chặn ở đây thì bài vẫn lên lịch được với cả lớp chưa có ca và không ai vào thi được.
     */
    private void requireClassTestScheduleReadiness(
            Assessment assessment,
            List<AssessmentSchedule> schedules) {
        requireSchedulesHaveRoomAndProctor(schedules);

        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByAssessmentId(assessment.getId());
        long withoutSchedule = candidates.stream()
            .filter(candidate -> candidate.getScheduleId() == null)
            .count();
        if (withoutSchedule > 0) {
            throw new IllegalStateException(
                "Còn " + withoutSchedule + " học sinh chưa được xếp vào ca thi");
        }
        long withoutPaper = candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .count();
        if (withoutPaper > 0) {
            throw new IllegalStateException("Còn " + withoutPaper + " học sinh chưa được gán đề");
        }
    }

    /**
     * The sessions of the assessment that are still in effect. A cancelled/moved session will never take place so it must not take part in
     * any scheduling condition: if their room/proctors were checked the assessment would be blocked
     * by an abandoned session, and if they were counted in "has a session" the assessment could be scheduled with no session
     * actually running. {@code findByAssessmentId} only filters out DELETED beforehand so the extra filtering has to happen here.
     */
    private List<AssessmentSchedule> effectiveSchedules(UUID assessmentId) {
        return assessmentScheduleRepository.findByAssessmentId(assessmentId).stream()
            .filter(schedule -> schedule.getStatus() != null && schedule.getStatus().isInEffect())
            .toList();
    }

    /** Ca thi còn hiệu lực nào cũng phải có phòng và ít nhất một giám khảo — đúng cho cả hai loại bài. */
    private void requireSchedulesHaveRoomAndProctor(
            List<AssessmentSchedule> schedules) {
        for (AssessmentSchedule schedule : schedules) {
            if (schedule.getSchoolRoomId() == null) {
                throw new IllegalStateException("Ca thi chưa được chọn phòng");
            }
            if (assessmentScheduleProctorRepository.countByScheduleId(schedule.getId()) == 0) {
                throw new IllegalStateException("Ca thi chưa có giám khảo");
            }
        }
    }

    /**
     * A centralized assessment used to be schedulable even with no session, no candidate and no paper
     * variant yet -- the assessment carried the SCHEDULED status although in practice nobody could take it.
     *
     * <p>The check here is one notch lighter than for a class test: it does NOT require each candidate to already have a session and a paper, because
     * that rule already lives in the step that publishes each session ({@code UpdateAssessmentScheduleStatusUseCase.publish}: a session must
     * have candidates and every candidate in it must have a paper). A centralized assessment therefore has to place candidates into sessions
     * ({@code AssignAssessmentCandidateSchedule}/{@code AutoFill}) and assign papers ({@code AssignAssessmentPapersUseCase},
     * which requires every paper to be LOCKED) BEFORE publishing the sessions -- both operations can run while the assessment is still DRAFT.
     * Here only the "empty" case that has no way of running is blocked.
     *
     * <p>In addition every session must already be published: a session still in DRAFT is not yet visible to students and proctors,
     * and an assessment that is "scheduled" with a session nobody can enter would be wrong.
     */
    private void requireCentralizedScheduleReadiness(Assessment assessment) {
        List<AssessmentSchedule> schedules = effectiveSchedules(assessment.getId());
        if (schedules.isEmpty()) {
            throw new IllegalStateException("Kỳ thi chưa có ca thi nào, không thể lên lịch");
        }
        requireSchedulesHaveRoomAndProctor(schedules);
        if (assessmentCandidateRepository.countByAssessmentId(assessment.getId()) == 0) {
            throw new IllegalStateException("Kỳ thi chưa có thí sinh nào, không thể lên lịch");
        }
        if (assessmentPaperRepository.findByAssessmentId(assessment.getId()).isEmpty()) {
            throw new IllegalStateException("Kỳ thi chưa có mã đề nào, không thể lên lịch");
        }
        // Publishing each session is a separate operation (UpdateAssessmentScheduleStatusUseCase) and does not require the status of the
        // assessment, so blocking here does not lock the flow: publish all the sessions first, then the assessment can be scheduled.
        long draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT)
            .count();
        if (draftScheduleCount > 0) {
            throw new IllegalStateException(
                "Còn " + draftScheduleCount + " ca thi chưa được công bố, không thể lên lịch kỳ thi");
        }
    }

    /**
     * This function used to push every DRAFT session to PUBLISHED by itself when the teacher pressed schedule -- so a class test and a
     * centralized assessment ran two opposite models, and the session changed status behind the user's back. Now it only CHECKS, it
     * does not write: publishing each session is a separate operation
     * ({@code UpdateAssessmentScheduleStatusUseCase}, regardless of the assessment kind), and scheduling the assessment only locks in
     * the outcome of that operation -- exactly like {@link #requireCentralizedScheduleReadiness}.
     */
    private void requireClassTestScheduleReady(Assessment assessment) {
        requireClassTestScheduleWindow(assessment);
        List<AssessmentSchedule> schedules = effectiveSchedules(assessment.getId());
        if (schedules.isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp chưa có lịch");
        }
        requireClassTestScheduleReadiness(assessment, schedules);
        long draftScheduleCount = schedules.stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT)
            .count();
        if (draftScheduleCount > 0) {
            throw new IllegalStateException(
                "Còn " + draftScheduleCount + " ca thi chưa được công bố, không thể lên lịch bài kiểm tra");
        }
    }

    private void requireClassTestScheduleWindow(Assessment assessment) {
        if (assessment.getOpenAt() == null || assessment.getCloseAt() == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài trước khi lên lịch");
        }
        if (!assessment.getOpenAt().isBefore(assessment.getCloseAt())) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
        // The session of a class test follows exactly the open/close window, so the window has to be enough for the attempt time.
        // Placed here (not in CreateClassTestUseCase.createDraftSchedule) because when the session is created
        // assessmentTimeDurationSecond is still the raw value the user typed, not yet passed through recalculate.
        if (assessment.isScheduleWindowShorterThanAssessmentTime(assessment.getOpenAt(), assessment.getCloseAt())) {
            throw new IllegalStateException(AssessmentScheduleWindowMessages.schedulesNoLongerFit(1, assessment));
        }
    }

    private void requireClassTestCanStart(Assessment assessment) {
        Instant now = Instant.now();
        if (assessment.getCloseAt() != null && !now.isBefore(assessment.getCloseAt())) {
            throw new IllegalStateException("Bài kiểm tra trên lớp đã quá thời gian đóng bài");
        }
    }

    /**
     * G.3: only allow publish when EVERY AssessmentCandidateResult of the assessment is exactly RELEASED
     * or INVALID (the 2 states "processed, ready to be finalized") - any other
     * status (PENDING_REVIEW not yet reviewed, FINAL/APPEALED/RE_GRADING/RETAKE_REQUIRED from the
     * appeal/suspicion flow) blocks publish until it is fully handled.
     *
     * <p>SOFT-DELETED SESSIONS/RESULTS (status DELETED) MUST BE EXCLUDED FROM BOTH LOOPS BELOW.
     * A soft delete does not remove the row, it only changes status -- {@code findByCandidateId}/{@code findByAssessmentId}
     * do not filter by status on their own, so without an explicit exclusion a student who failed the attempt, had the session
     * deleted and then retook it (DeleteAssessmentSessionUseCase) would block publishing OF THE WHOLE ASSESSMENT
     * forever: DELETED never turns into RELEASED/INVALID by itself (AssessmentCandidateResultFinalizationService.
     * finalizeForPublish also deliberately skips DELETED, see "out of scope, kept as is" there),
     * and a deleted session obviously has no NEW result to fill in. Measured for real: blocked on exactly
     * the line "N results are still not in RELEASED or INVALID" even though the assessment has no result
     * really hanging -- the replacement session was graded, only the deleted OLD session stands still.
     */
    private void requirePublishReadiness(UUID assessmentId) {
        long missingResultCount = assessmentCandidateRepository.findByAssessmentId(assessmentId).stream()
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .flatMap(candidate -> assessmentSessionRepository.findByCandidateId(candidate.getId()).stream())
            .filter(session -> assessmentId.equals(session.getAssessmentId()))
            .filter(session -> session.getStatus() != AssessmentSessionStatus.IN_PROGRESS
                && session.getStatus() != AssessmentSessionStatus.INTERRUPTED
                // A deleted session never "has a result" (its result was deleted along with it,
                // see DeleteAssessmentSessionUseCase), but it is NOT what is blocking publish -- it is
                // skipped on purpose, not missed.
                && session.getStatus() != AssessmentSessionStatus.DELETED)
            .filter(session -> assessmentCandidateResultRepository.findBySessionId(session.getId()).isEmpty())
            .count();
        if (missingResultCount > 0) {
            throw new IllegalStateException(
                "Còn " + missingResultCount + " phiên thi chưa có kết quả, không thể công bố kết quả");
        }

        long notReadyCount = assessmentCandidateResultRepository.findByAssessmentId(assessmentId).stream()
            .filter(result -> result.getStatus() != AssessmentCandidateResultStatus.RELEASED
                && result.getStatus() != AssessmentCandidateResultStatus.INVALID
                && result.getStatus() != AssessmentCandidateResultStatus.DELETED)
            .count();
        if (notReadyCount > 0) {
            throw new IllegalStateException(
                "Còn " + notReadyCount + " kết quả chưa ở trạng thái RELEASED hoặc INVALID, không thể công bố kết quả");
        }
    }

    /**
     * G.3: finalizes the result of every AssessmentCandidateResult of the assessment right when it has just moved to
     * RESULTS_PUBLISHED - INVALID -> FAILED (score forced to 0), RELEASED/FINAL -> PASSED/FAILED
     * by passingScore if the policy has a threshold, or -> FINAL if there is no threshold (the school
     * chooses PASSED/FAILED later through decideAssessmentCandidateResultOutcome). See
     * AssessmentCandidateResultFinalizationService.finalizeForPublish.
     */
    private void finalizePassFailForAssessment(Assessment assessment) {
        BigDecimal passingScore = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
                .map(policy -> policy.getPassingScore())
                .orElse(null);

        for (AssessmentCandidateResult result : assessmentCandidateResultRepository.findByAssessmentId(assessment.getId())) {
            if (assessmentCandidateResultFinalizationService.finalizeForPublish(result, passingScore)) {
                assessmentCandidateResultRepository.save(result);
            }
        }
    }

    private void lockClassTestPapers(UUID assessmentId) {
        for (AssessmentPaper paper : assessmentPaperRepository.findByAssessmentId(assessmentId)) {
            if (paper.getStatus() != AssessmentPaperStatus.LOCKED) {
                paper.setStatus(AssessmentPaperStatus.LOCKED);
                assessmentPaperRepository.save(paper);
            }
        }
    }
}
