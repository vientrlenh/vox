package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.AssessmentAttemptForceEndRequestedExternalEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ForceEndAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
public class ForceEndAssessmentSessionUseCase implements IUseCase<ForceEndAssessmentSessionCommand, UUID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForceEndAssessmentSessionUseCase.class);

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final AssessmentCandidateResultFinalizationService assessmentCandidateResultFinalizationService;
    private final ExternalEventPublisherPort externalEventPublisherPort;

    public ForceEndAssessmentSessionUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentSessionModerationAccessService moderationAccessService,
            AssessmentCandidateResultFinalizationService assessmentCandidateResultFinalizationService,
            ExternalEventPublisherPort externalEventPublisherPort) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.moderationAccessService = moderationAccessService;
        this.assessmentCandidateResultFinalizationService = assessmentCandidateResultFinalizationService;
        this.externalEventPublisherPort = externalEventPublisherPort;
    }

    @Override
    @Transactional
    public java.util.UUID execute(ForceEndAssessmentSessionCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(assessment, candidate);
        // G.4: mốc chặn cứng duy nhất là RESULTS_PUBLISHED - trước đó (kể cả sau khi kỳ thi
        // đã đóng) vẫn cho phát hiện vi phạm muộn qua soát lại log/video.
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố kết quả, không thể thay đổi nữa");
        }
        boolean isLiveSession = session.getStatus() == AssessmentSessionStatus.IN_PROGRESS
            || session.getStatus() == AssessmentSessionStatus.INTERRUPTED;
        boolean isAlreadyGraded = session.getStatus() == AssessmentSessionStatus.GRADED;
        if (!isLiveSession && !isAlreadyGraded) {
            throw new IllegalStateException("Chỉ có thể buộc kết thúc phiên thi đang diễn ra hoặc đã chấm xong");
        }

        Instant now = Instant.now();
        UUID currentUserId = moderationAccessService.getCurrentUserId();
        String normalizedReason = StringNormalization.trimAndCollapseSpaces(input.reason());
        if (normalizedReason == null || normalizedReason.isEmpty()) {
            throw new IllegalArgumentException("Cần nhập lý do khi buộc kết thúc phiên thi");
        }

        session.setFlagged(true);
        session.setFlagReason(normalizedReason);
        if (isLiveSession) {
            session.setStatus(AssessmentSessionStatus.INTERRUPTED);
        }

        candidate.setBlockedAt(now);
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(currentUserId);

        assessmentSessionRepository.save(session);
        assessmentCandidateRepository.save(candidate);

        // DELIBERATELY does NOT fill in the empty answer here.
        //
        // The previous version called backfill() at this point, based on a WRONG assumption: that a forced end
        // is final.
        //
        // It is not final. Unblocking moves the session INTERRUPTED back to IN_PROGRESS
        // (VerifyAssessmentScheduleOtpUseCase#findResumableSession) and the candidate continues with exactly the question that was just
        // filled in -- producing a SECOND response for the same paper item. The filled-in version carries a random id while the
        // real one carries the deterministic id generated by Python, so the two rows can never be merged.
        //
        // Measured 2026-08-18, session 01a012cf: a paper of 2 questions but 3 responses. Question 2 carried both an empty
        // 0-score row (filled in at 03:01:32) and the real spoken answer 8.38 (03:04:04). The score of that part was dragged down to
        // average(0, 8.38) = 4.19, the attempt total dropped from 8.06 to 5.96 and fell two grade levels --
        // the candidate lost 2.1 points. The grading screen showed 3 tabs, and because the form is keyed by paperItemId the empty
        // tab even borrowed the criterion scores of the real question.
        //
        // Filling in 0 now happens at the truly final moment: when submitting (SubmitAssessmentSessionUseCase)
        // or when the teacher finalizes for publishing (UpholdResultUseCase, which checks that the session is no longer
        // RESUMABLE). The original worry -- the result page breaking for lack of an evaluation -- is gone:
        // ViewAssessmentSessionResultUseCase builds the question list straight from the responses and leaves the score null.

        if (isAlreadyGraded) {
            // G.4 case 1: phát hiện vi phạm muộn cho 1 bài đã chấm xong - chốt ngay
            // INVALID/0 điểm, không đợi tới lúc kỳ thi công bố kết quả.
            assessmentCandidateResultFinalizationService.invalidateNow(session.getId(), currentUserId);
            return session.getId();
        }

        try {
            externalEventPublisherPort.publish(
                new AssessmentAttemptForceEndRequestedExternalEvent(session.getId().toString(), normalizedReason)
            );
        } catch (RuntimeException ex) {
            LOGGER.warn("Không thể publish sự kiện buộc kết thúc cho session {}", session.getId(), ex);
        }
        return session.getId();
    }
}
