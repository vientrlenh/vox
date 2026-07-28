package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamSessionRemainingTimeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Ghi lại đồng hồ đếm ngược mà client báo về, để lần vào lại phiên thi không đếm ngược từ đầu.
 *
 * <p>Giá trị này bắt buộc phải do client cung cấp: đồng hồ phía WPF không trừ giây khi avatar đang
 * nói, nên server không thể tự tính từ {@code startedAt + duration}. Đổi lại, nó là dữ liệu do máy
 * học viên gửi lên và phải được coi là không đáng tin - hai ràng buộc dưới đây là thứ ngăn nó trở
 * thành API tự gia hạn thời gian thi:
 *
 * <ul>
 *   <li>chặn trên bằng thời lượng cấu hình của kỳ thi, để một giá trị lớn bất thường không có tác
 *       dụng gì;
 *   <li>chỉ cho đồng hồ đi lùi (thực thi bằng câu UPDATE có điều kiện ở
 *       {@code ExamSessionRepository#checkpointRemainingSeconds}).
 * </ul>
 *
 * <p>Trả về giá trị thực sự đang được lưu, không phải giá trị client gửi - client biết được yêu cầu
 * của mình có bị từ chối hay không thay vì phải đoán.
 */
@Service
public class UpdateExamSessionRemainingTimeUseCase
        implements IUseCase<UpdateExamSessionRemainingTimeCommand, Integer> {

    private final UserContextPort userContextPort;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;

    public UpdateExamSessionRemainingTimeUseCase(
            UserContextPort userContextPort,
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository) {
        this.userContextPort = userContextPort;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
    }

    @Override
    @Transactional
    public Integer execute(UpdateExamSessionRemainingTimeCommand input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        // RESUMABLE chứ không phải findById: checkpoint cho một phiên đã nộp hoặc đã chấm là vô
        // nghĩa, và nhận nó vào sẽ làm bẩn dữ liệu của lần chấm.
        var session = examSessionRepository.findByIdAndResumable(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã kết thúc"));

        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy học viên thi"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        examSessionRepository.checkpointRemainingSeconds(session.getId(), clamp(session, input.remainingSeconds()));

        return examSessionRepository.findById(session.getId())
            .map(s -> s.getRemainingSeconds())
            .orElse(null);
    }

    /**
     * Ép giá trị client gửi về khoảng hợp lệ. Chặn trên là thời lượng cấu hình của kỳ thi; nếu kỳ
     * thi chưa đặt thời lượng thì bỏ qua chặn trên và chỉ còn dựa vào ràng buộc "chỉ đi lùi" ở tầng
     * DB - vẫn đủ để giá trị không bao giờ tăng trong một phiên thi.
     */
    private int clamp(ExamSession session, int remainingSeconds) {
        var value = Math.max(0, remainingSeconds);
        var ceiling = examRepository.findById(session.getExamId())
            .map(e -> e.getExamTimeDurationSecond())
            .orElse(null);
        if (ceiling != null && value > ceiling) {
            return ceiling;
        }
        return value;
    }
}
