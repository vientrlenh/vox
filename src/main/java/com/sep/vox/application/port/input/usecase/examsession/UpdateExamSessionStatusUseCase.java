package com.sep.vox.application.port.input.usecase.examsession;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class UpdateExamSessionStatusUseCase implements IUseCase<UpdateExamSessionStatusCommand, ExamSessionResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateExamSessionStatusUseCase.class);

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final SubmitExamSessionUseCase submitExamSessionUseCase;

    public UpdateExamSessionStatusUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            AiUsageRecordRepository aiUsageRecordRepository,
            SubmitExamSessionUseCase submitExamSessionUseCase) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.aiUsageRecordRepository = aiUsageRecordRepository;
        this.submitExamSessionUseCase = submitExamSessionUseCase;
    }

    @Override
    @Transactional
    public ExamSessionResponse execute(UpdateExamSessionStatusCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        // B.4: race - job hết-ca-thi vừa set EXPIRED ngay trước khi client kịp gửi SUBMITTED.
        // Coi là no-op thành công, giữ nguyên EXPIRED (việc chấm đã/sẽ diễn ra qua đường EXPIRED),
        // không throw để học sinh không thấy lỗi ngay lúc tưởng đã nộp bài xong.
        if (session.getStatus() == ExamSessionStatus.EXPIRED && input.status() == ExamSessionStatus.SUBMITTED) {
            return CreateExamSessionUseCase.toResponse(session);
        }

        validateTransition(session.getStatus(), input.status());
        session.setStatus(input.status());
        if ((input.status() == ExamSessionStatus.SUBMITTED || input.status() == ExamSessionStatus.EXPIRED)
                && session.getSubmittedAt() == null) {
            session.setSubmittedAt(Instant.now());
        }
        applyGradingFailure(session, input);

        examSessionRepository.save(session);
        if (input.status() == ExamSessionStatus.SUBMITTED && canSubmitImmediately(session)) {
            submitExamSessionUseCase.execute(new SubmitExamSessionCommand(session.getId()));
        }

        return CreateExamSessionUseCase.toResponse(
            examSessionRepository.findById(session.getId()).orElse(session)
        );
    }

    /**
     * Lý do chấm hỏng sống đúng bằng lần hỏng đó.
     *
     * <p>RỜI khỏi GRADING_FAILED là phải xóa: đường duy nhất đi ra là sang GRADING (chấm lại), và
     * nếu lần đó thành công mà thông điệp cũ còn nằm lại thì trang phân loại vẫn đếm phiên này vào
     * nhóm sự cố — người trực sẽ xử lý lại đúng thứ vừa xong. Xóa ngay tại đây, chứ không đợi tới
     * lúc chấm xong, vì giữa hai mốc đó phiên đang GRADING nên ràng buộc
     * {@code chk_exam_sessions_grading_error_only_when_failed} ở DB sẽ từ chối cả bản ghi.
     *
     * <p>VÀO GRADING_FAILED mà {@code gradingFailure} null thì cũng ghi null: đó là nhánh DLT, và
     * "không rõ vì sao" là câu trả lời đúng chứ không phải dữ liệu thiếu.
     */
    private void applyGradingFailure(ExamSession session,
            UpdateExamSessionStatusCommand input) {
        if (input.status() != ExamSessionStatus.GRADING_FAILED) {
            session.clearGradingFailure();
            return;
        }
        session.setGradingError(input.gradingFailure() == null ? null : input.gradingFailure().error());
        session.setGradingRetryCount(input.gradingFailure() == null ? null : input.gradingFailure().retryCount());
        waiveFailedRoundCost(session.getId());
    }

    /**
     * Lượt chấm vừa hỏng KHÔNG được thu tiền của trường.
     *
     * <p>Quy tắc là "thu cho phần việc TẠO RA KẾT QUẢ DÙNG ĐƯỢC", không phải "hỏng thì miễn". Lượt
     * hỏng không để lại dòng {@code exam_candidate_results} nào, nên trường không nhận được gì —
     * khác hẳn một lượt luyện nói đã trả lời học sinh xong, vốn vẫn bị thu dù chuyện gì xảy ra sau
     * đó (xem {@code SubmitPracticeTurnUseCase}).
     *
     * <p>Miễn NGAY tại đây thay vì lúc chấm lại: để tới đó thì tiền phụ thuộc vào việc có người đi
     * khắc phục hay không — bỏ mặc thì miễn phí, đi sửa thì bị tính tiền cho cả lượt hỏng. Đúng
     * hành vi ta muốn khuyến khích lại là hành vi duy nhất bị phạt.
     *
     * <p>Chỉ động vào dòng CHƯA NGÃ NGŨ. Lượt chấm thành công trước đó (nếu có) đã đóng dấu
     * {@code charged_at} và phải giữ nguyên: miễn đè lên chúng là hoàn tiền cho phần việc đã giao đủ.
     */
    private void waiveFailedRoundCost(UUID sessionId) {
        var waived = aiUsageRecordRepository.markWaivedByExamSessionId(sessionId, Instant.now());
        if (waived > 0) {
            LOGGER.info("Miễn {} dòng chi phí AI của lượt chấm hỏng ở phiên {}", waived, sessionId);
        }
    }

    /**
     * B.0: chỉ blockedAt quyết định hoãn/chấm ngay - flagged chỉ ảnh hưởng việc
     * có tính vào kết quả chính thức hay không (mục G/I), không hoãn việc chấm AI.
     */
    private boolean canSubmitImmediately(ExamSession session) {
        return examCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getBlockedAt() == null)
            .orElse(false);
    }

    private void validateTransition(ExamSessionStatus from, ExamSessionStatus to) {
        if (from == null || to == null) {
            throw new IllegalStateException("Trạng thái phiên thi không hợp lệ");
        }
        if (from == to) {
            return;
        }
        if (isAllowedTransition(from, to)) {
            return;
        }
        throw new IllegalStateException("Không thể chuyển trạng thái phiên thi từ " + from + " sang " + to);
    }

    private boolean isAllowedTransition(ExamSessionStatus from, ExamSessionStatus to) {
        return switch (from) {
            case IN_PROGRESS -> to == ExamSessionStatus.SUBMITTED
                || to == ExamSessionStatus.INTERRUPTED
                || to == ExamSessionStatus.EXPIRED;
            case INTERRUPTED -> to == ExamSessionStatus.IN_PROGRESS || to == ExamSessionStatus.EXPIRED;
            case SUBMITTED -> to == ExamSessionStatus.GRADING;
            case EXPIRED -> to == ExamSessionStatus.GRADING;
            case GRADING -> to == ExamSessionStatus.GRADED || to == ExamSessionStatus.GRADING_FAILED;
            case GRADING_FAILED -> to == ExamSessionStatus.GRADING;
            case GRADED -> to == ExamSessionStatus.GRADING;
            // DELETED là điểm dừng, không đi tiếp đâu được: phục hồi một phiên đã xoá là thao tác
            // sửa dữ liệu trực tiếp (xoá mềm không có đường phục hồi qua API), không phải một bước
            // chuyển trạng thái bình thường.
            case DELETED -> false;
        };
    }
}
