package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentAttemptForceEndRequestedExternalEvent;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.ForceEndAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

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
        var session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        var candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi"));
        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(assessment, candidate);
        // G.4: mốc chặn cứng duy nhất là RESULTS_PUBLISHED - trước đó (kể cả sau khi kỳ thi
        // đã đóng) vẫn cho phát hiện vi phạm muộn qua soát lại log/video.
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố kết quả, không thể thay đổi nữa");
        }
        var isLiveSession = session.getStatus() == AssessmentSessionStatus.IN_PROGRESS
            || session.getStatus() == AssessmentSessionStatus.INTERRUPTED;
        var isAlreadyGraded = session.getStatus() == AssessmentSessionStatus.GRADED;
        if (!isLiveSession && !isAlreadyGraded) {
            throw new IllegalStateException("Chỉ có thể buộc kết thúc phiên thi đang diễn ra hoặc đã chấm xong");
        }

        var now = Instant.now();
        var currentUserId = moderationAccessService.getCurrentUserId();
        var normalizedReason = StringNormalization.trimAndCollapseSpaces(input.reason());
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

        // CỐ Ý KHÔNG lấp câu trống ở đây.
        //
        // Bản trước có gọi backfill() tại chỗ này, dựa trên một giả định SAI: rằng buộc kết thúc
        // là chung cuộc.
        //
        // Nó không chung cuộc. Gỡ chặn đưa phiên INTERRUPTED trở lại IN_PROGRESS
        // (VerifyAssessmentScheduleOtpUseCase#findResumableSession) và thí sinh làm tiếp đúng câu vừa bị
        // lấp -- sinh response THỨ HAI cho cùng một paper item. Bản lấp mang id ngẫu nhiên còn bản
        // thật mang id tất định do Python sinh, nên hai dòng không bao giờ hoà được vào nhau.
        //
        // Đo được 2026-08-18, phiên 01a012cf: đề 2 câu nhưng có 3 response. Câu 2 mang cả bản rỗng
        // 0 điểm (lấp lúc 03:01:32) lẫn bản nói thật 8.38 (03:04:04). Điểm phần đó bị kéo còn
        // trung bình(0 , 8.38) = 4.19, tổng bài rớt từ 8.06 xuống 5.96 và tụt hai bậc xếp loại --
        // thí sinh mất 2.1 điểm. Màn chấm thì hiện 3 tab, và vì form khoá theo paperItemId nên tab
        // rỗng còn mượn luôn điểm tiêu chí của câu thật.
        //
        // Việc lấp 0 nay nằm ở đúng thời điểm chung cuộc: lúc nộp bài (SubmitAssessmentSessionUseCase)
        // hoặc lúc giáo viên chốt để công bố (UpholdResultUseCase, có kiểm phiên đã hết RESUMABLE
        // chưa). Còn nỗi lo ban đầu -- trang kết quả vỡ vì thiếu evaluation -- không còn:
        // ViewAssessmentSessionResultUseCase dựng danh sách câu thẳng từ response và để điểm null.

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
