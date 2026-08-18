package com.sep.vox.application.port.input.usecase.examsession;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.ExamAttemptForceEndRequestedExternalEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ForceEndExamSessionCommand;
import com.sep.vox.application.port.input.service.ExamCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class ForceEndExamSessionUseCase implements IUseCase<ForceEndExamSessionCommand, UUID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForceEndExamSessionUseCase.class);

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionModerationAccessService moderationAccessService;
    private final ExamCandidateResultFinalizationService examCandidateResultFinalizationService;
    private final ExternalEventPublisherPort externalEventPublisherPort;

    public ForceEndExamSessionUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamSessionModerationAccessService moderationAccessService,
            ExamCandidateResultFinalizationService examCandidateResultFinalizationService,
            ExternalEventPublisherPort externalEventPublisherPort) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.moderationAccessService = moderationAccessService;
        this.examCandidateResultFinalizationService = examCandidateResultFinalizationService;
        this.externalEventPublisherPort = externalEventPublisherPort;
    }

    @Override
    @Transactional
    public java.util.UUID execute(ForceEndExamSessionCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(exam, candidate);
        // G.4: mốc chặn cứng duy nhất là RESULTS_PUBLISHED - trước đó (kể cả sau khi kỳ thi
        // đã đóng) vẫn cho phát hiện vi phạm muộn qua soát lại log/video.
        if (exam.getStatus() == ExamStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố kết quả, không thể thay đổi nữa");
        }
        var isLiveSession = session.getStatus() == ExamSessionStatus.IN_PROGRESS
            || session.getStatus() == ExamSessionStatus.INTERRUPTED;
        var isAlreadyGraded = session.getStatus() == ExamSessionStatus.GRADED;
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
            session.setStatus(ExamSessionStatus.INTERRUPTED);
        }

        candidate.setBlockedAt(now);
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(currentUserId);

        examSessionRepository.save(session);
        examCandidateRepository.save(candidate);

        // CỐ Ý KHÔNG lấp câu trống ở đây.
        //
        // Bản trước có gọi backfill() tại chỗ này, dựa trên một giả định SAI: rằng buộc kết thúc
        // là chung cuộc.
        //
        // Nó không chung cuộc. Gỡ chặn đưa phiên INTERRUPTED trở lại IN_PROGRESS
        // (VerifyExamScheduleOtpUseCase#findResumableSession) và thí sinh làm tiếp đúng câu vừa bị
        // lấp -- sinh response THỨ HAI cho cùng một paper item. Bản lấp mang id ngẫu nhiên còn bản
        // thật mang id tất định do Python sinh, nên hai dòng không bao giờ hoà được vào nhau.
        //
        // Đo được 2026-08-18, phiên 01a012cf: đề 2 câu nhưng có 3 response. Câu 2 mang cả bản rỗng
        // 0 điểm (lấp lúc 03:01:32) lẫn bản nói thật 8.38 (03:04:04). Điểm phần đó bị kéo còn
        // trung bình(0 , 8.38) = 4.19, tổng bài rớt từ 8.06 xuống 5.96 và tụt hai bậc xếp loại --
        // thí sinh mất 2.1 điểm. Màn chấm thì hiện 3 tab, và vì form khoá theo paperItemId nên tab
        // rỗng còn mượn luôn điểm tiêu chí của câu thật.
        //
        // Việc lấp 0 nay nằm ở đúng thời điểm chung cuộc: lúc nộp bài (SubmitExamSessionUseCase)
        // hoặc lúc giáo viên chốt để công bố (UpholdResultUseCase, có kiểm phiên đã hết RESUMABLE
        // chưa). Còn nỗi lo ban đầu -- trang kết quả vỡ vì thiếu evaluation -- không còn:
        // ViewExamSessionResultUseCase dựng danh sách câu thẳng từ response và để điểm null.

        if (isAlreadyGraded) {
            // G.4 case 1: phát hiện vi phạm muộn cho 1 bài đã chấm xong - chốt ngay
            // INVALID/0 điểm, không đợi tới lúc kỳ thi công bố kết quả.
            examCandidateResultFinalizationService.invalidateNow(session.getId(), currentUserId);
            return session.getId();
        }

        try {
            externalEventPublisherPort.publish(
                new ExamAttemptForceEndRequestedExternalEvent(session.getId().toString(), normalizedReason)
            );
        } catch (RuntimeException ex) {
            LOGGER.warn("Không thể publish sự kiện buộc kết thúc cho session {}", session.getId(), ex);
        }
        return session.getId();
    }
}
