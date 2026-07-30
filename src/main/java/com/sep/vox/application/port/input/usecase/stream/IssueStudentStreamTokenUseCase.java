package com.sep.vox.application.port.input.usecase.stream;

import java.time.OffsetDateTime;
import java.util.List;

import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.IssueStudentStreamTokenCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.stream.IssueStudentStreamTokenResponse;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSession;

@Service
public class IssueStudentStreamTokenUseCase implements IUseCase<IssueStudentStreamTokenCommand, IssueStudentStreamTokenResponse> {

    private final UserContextPort userContextPort;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final StreamTokenProvider streamTokenProvider; 


    public IssueStudentStreamTokenUseCase(UserContextPort userContextPort, 
                                            ExamScheduleRepository examScheduleRepository, 
                                            ExamRepository examRepository, 
                                            ExamCandidateRepository examCandidateRepository, 
                                            ExamSessionRepository examSessionRepository, 
                                            StreamTokenProvider streamTokenProvider) {
        this.userContextPort = userContextPort;
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.streamTokenProvider = streamTokenProvider;
    }

    @Override
    @Transactional
    public IssueStudentStreamTokenResponse execute(IssueStudentStreamTokenCommand input) {
        var command = normalizeCommand(input);

        var now = OffsetDateTime.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        var examSession = examSessionRepository.findByIdAndResumable(command.examSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã hết hạn"));

        var exam = examRepository.findById(examSession.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        var candidate = examCandidateRepository.findById(examSession.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy học viên thi"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var schedule = examScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch thi"));

        // Sau toàn bộ khâu xác thực, không phải trước: bước này GHI vào phiên thi (chốt loại
        // stream), nên nếu chạy sớm hơn thì một người không sở hữu phiên thi vẫn kịp chốt hộ
        // loại stream trước khi bị chặn ở kiểm tra quyền bên trên.
        var streamTypes = resolveStreamTypes(exam, examSession, command.streamType());

        var windowStart = schedule.getStartDate();
        var windowEnd = schedule.getEndDate();

        var token = streamTokenProvider.generateStreamToken(
            userId.toString(),
            candidate.getId().toString(), 
            schedule.getId().toString(),  
            exam.getId().toString(), 
            examSession.getId().toString(),
            streamTypes, 
            windowStart, 
            windowEnd
        );

        return new IssueStudentStreamTokenResponse(token, schedule.getId(), examSession.getId(), streamTypes, windowEnd);
    }


    private IssueStudentStreamTokenCommand normalizeCommand(IssueStudentStreamTokenCommand input) {
        // normalizeCode = strip + uppercase. Chấp nhận cả "camera" lẫn "CAMERA" vì phía client
        // (vox-streaming, WPF) dùng chữ thường cho cùng những giá trị này - ép về một dạng ở đây
        // rẻ hơn nhiều so với việc bắt mọi client phải nhớ đúng kiểu chữ.
        return new IssueStudentStreamTokenCommand(
            input.examSessionId(),
            StringNormalization.normalizeCode(input.streamType())
        );
    }


    /**
     * Trả về danh sách stream type (chữ thường, đúng dạng vox-streaming chờ đợi) cho phiên thi,
     * đồng thời chốt lựa chọn ở lần gọi đầu tiên.
     *
     * <p>Một phiên thi phát token nhiều lần: lúc vào thi, lúc reconnect, và mỗi lần
     * UploadCredentialRefresher gia hạn. Nếu mỗi lần đều tính lại từ request thì học viên trong kỳ
     * thi cấu hình {@code ANY} có thể bắt đầu bằng camera + màn hình rồi phát lại token với mỗi
     * camera, tắt hẳn phần ghi màn hình giữa chừng mà không để lại dấu vết. Vì vậy lần đầu ghi
     * xuống DB, các lần sau đọc lại từ đó.
     */
    private List<String> resolveStreamTypes(Exam exam, ExamSession examSession, String requestedStreamType) {
        if (exam.getRequiredStreamType() == null) {
            throw new IllegalArgumentException("Kỳ thi không hỗ trợ stream");
        }

        var chosenStreamType = examSession.getChosenStreamType();
        if (chosenStreamType == null) {
            chosenStreamType = lockChosenStreamType(examSession, resolveFirstChoice(exam, requestedStreamType));
        }

        if (requestedStreamType != null && !requestedStreamType.equals(chosenStreamType.name())) {
            throw new ForbiddenException(
                "Phiên thi đã chốt loại stream " + chosenStreamType.name() + ", không thể đổi giữa chừng"
            );
        }
        return toStreamTypes(chosenStreamType);
    }

    /**
     * Ghi lựa chọn xuống phiên thi. Nếu thua một request song song (0 dòng được cập nhật) thì đọc
     * lại giá trị đã thắng thay vì ghi đè - phía gọi sẽ đối chiếu giá trị đó với yêu cầu ban đầu.
     */
    private ExamRequiredStreamType lockChosenStreamType(ExamSession examSession, ExamRequiredStreamType chosenStreamType) {
        if (examSessionRepository.lockChosenStreamType(examSession.getId(), chosenStreamType) > 0) {
            return chosenStreamType;
        }
        return examSessionRepository.findById(examSession.getId())
            .map(session -> session.getChosenStreamType())
            .orElse(chosenStreamType);
    }

    /**
     * Loại stream cho lần phát token đầu tiên. Bỏ trống nghĩa là "theo cấu hình kỳ thi" - client
     * không cần biết {@code requiredStreamType} là gì mới xin được token.
     */
    private ExamRequiredStreamType resolveFirstChoice(Exam exam, String requestedStreamType) {
        if (requestedStreamType == null || requestedStreamType.isBlank()) {
            return exam.getRequiredStreamType();
        }
        switch (requestedStreamType) {
            case "CAMERA":
                if (isInvalidRequestedIndividualStreamType(exam, ExamRequiredStreamType.SCREEN)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream screen");
                }
                return ExamRequiredStreamType.CAMERA;
            case "SCREEN":
                if (isInvalidRequestedIndividualStreamType(exam, ExamRequiredStreamType.CAMERA)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream camera");
                }
                return ExamRequiredStreamType.SCREEN;
            case "CAMERA_AND_SCREEN":
                if (!exam.getRequiredStreamType().equals(ExamRequiredStreamType.CAMERA_AND_SCREEN)) {
                    throw new IllegalArgumentException("Kỳ thi không yêu cầu đồng thời camera và màn hình");
                }
                return ExamRequiredStreamType.CAMERA_AND_SCREEN;
            default:
                throw new IllegalArgumentException("Loại stream không được hỗ trợ: " + requestedStreamType);
        }
    }

    private static List<String> toStreamTypes(ExamRequiredStreamType streamType) {
        return switch (streamType) {
            case CAMERA -> List.of("camera");
            case SCREEN -> List.of("screen");
            case CAMERA_AND_SCREEN -> List.of("camera", "screen");
        };
    }

    private boolean isInvalidRequestedIndividualStreamType(Exam exam, ExamRequiredStreamType requiredStreamType) {
        return exam.getRequiredStreamType().equals(requiredStreamType) || (exam.getRequiredStreamType().equals(ExamRequiredStreamType.CAMERA_AND_SCREEN) && (exam.getStreamTypePermission() == null || exam.getStreamTypePermission().equals(ExamStreamTypePermission.ALL)));
    }
}
