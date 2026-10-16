package com.sep.vox.application.port.input.usecase.stream;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.IssueStudentStreamTokenCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.stream.IssueStudentStreamTokenResponse;
import com.sep.vox.application.shared.StringNormalization;

@Service
public class IssueStudentStreamTokenUseCase implements IUseCase<IssueStudentStreamTokenCommand, IssueStudentStreamTokenResponse> {

    private final UserContextPort userContextPort;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final StreamTokenProvider streamTokenProvider; 


    public IssueStudentStreamTokenUseCase(UserContextPort userContextPort, 
                                            AssessmentScheduleRepository assessmentScheduleRepository, 
                                            AssessmentRepository assessmentRepository, 
                                            AssessmentCandidateRepository assessmentCandidateRepository, 
                                            AssessmentSessionRepository assessmentSessionRepository, 
                                            StreamTokenProvider streamTokenProvider) {
        this.userContextPort = userContextPort;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.streamTokenProvider = streamTokenProvider;
    }

    @Override
    @Transactional
    public IssueStudentStreamTokenResponse execute(IssueStudentStreamTokenCommand input) {
        var command = normalizeCommand(input);

        var now = Instant.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        var assessmentSession = assessmentSessionRepository.findByIdAndResumable(command.assessmentSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã hết hạn"));

        var assessment = assessmentRepository.findById(assessmentSession.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        var candidate = assessmentCandidateRepository.findById(assessmentSession.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên thi"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var schedule = assessmentScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch thi"));

        // Sau toàn bộ khâu xác thực, không phải trước: bước này GHI vào phiên thi (chốt loại
        // stream), nên nếu chạy sớm hơn thì một người không sở hữu phiên thi vẫn kịp chốt hộ
        // loại stream trước khi bị chặn ở kiểm tra quyền bên trên.
        var streamTypes = resolveStreamTypes(assessment, assessmentSession, command.streamType());

        var windowStart = schedule.getStartDate();
        var windowEnd = schedule.getEndDate();

        var token = streamTokenProvider.generateStreamToken(
            userId.toString(),
            candidate.getId().toString(), 
            schedule.getId().toString(),  
            assessment.getId().toString(), 
            assessmentSession.getId().toString(),
            streamTypes, 
            windowStart, 
            windowEnd
        );

        return new IssueStudentStreamTokenResponse(token, schedule.getId(), assessmentSession.getId(), streamTypes, windowEnd);
    }


    private IssueStudentStreamTokenCommand normalizeCommand(IssueStudentStreamTokenCommand input) {
        // normalizeCode = strip + uppercase. Chấp nhận cả "camera" lẫn "CAMERA" vì phía client
        // (vox-streaming, WPF) dùng chữ thường cho cùng những giá trị này - ép về một dạng ở đây
        // rẻ hơn nhiều so với việc bắt mọi client phải nhớ đúng kiểu chữ.
        return new IssueStudentStreamTokenCommand(
            input.assessmentSessionId(),
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
    private List<String> resolveStreamTypes(Assessment assessment, AssessmentSession assessmentSession, String requestedStreamType) {
        if (assessment.getRequiredStreamType() == null) {
            throw new IllegalArgumentException("Kỳ thi không hỗ trợ stream");
        }

        var chosenStreamType = assessmentSession.getChosenStreamType();
        if (chosenStreamType == null) {
            chosenStreamType = lockChosenStreamType(assessmentSession, resolveFirstChoice(assessment, requestedStreamType));
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
    private AssessmentRequiredStreamType lockChosenStreamType(AssessmentSession assessmentSession, AssessmentRequiredStreamType chosenStreamType) {
        if (assessmentSessionRepository.lockChosenStreamType(assessmentSession.getId(), chosenStreamType) > 0) {
            return chosenStreamType;
        }
        return assessmentSessionRepository.findById(assessmentSession.getId())
            .map(session -> session.getChosenStreamType())
            .orElse(chosenStreamType);
    }

    /**
     * Loại stream cho lần phát token đầu tiên. Bỏ trống nghĩa là "theo cấu hình kỳ thi" - client
     * không cần biết {@code requiredStreamType} là gì mới xin được token.
     */
    private AssessmentRequiredStreamType resolveFirstChoice(Assessment assessment, String requestedStreamType) {
        if (requestedStreamType == null || requestedStreamType.isBlank()) {
            return assessment.getRequiredStreamType();
        }
        switch (requestedStreamType) {
            case "CAMERA":
                if (isInvalidRequestedIndividualStreamType(assessment, AssessmentRequiredStreamType.SCREEN)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream screen");
                }
                return AssessmentRequiredStreamType.CAMERA;
            case "SCREEN":
                if (isInvalidRequestedIndividualStreamType(assessment, AssessmentRequiredStreamType.CAMERA)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream camera");
                }
                return AssessmentRequiredStreamType.SCREEN;
            case "CAMERA_AND_SCREEN":
                if (!assessment.getRequiredStreamType().equals(AssessmentRequiredStreamType.CAMERA_AND_SCREEN)) {
                    throw new IllegalArgumentException("Kỳ thi không yêu cầu đồng thời camera và màn hình");
                }
                return AssessmentRequiredStreamType.CAMERA_AND_SCREEN;
            default:
                throw new IllegalArgumentException("Loại stream không được hỗ trợ: " + requestedStreamType);
        }
    }

    private static List<String> toStreamTypes(AssessmentRequiredStreamType streamType) {
        return switch (streamType) {
            case CAMERA -> List.of("camera");
            case SCREEN -> List.of("screen");
            case CAMERA_AND_SCREEN -> List.of("camera", "screen");
        };
    }

    private boolean isInvalidRequestedIndividualStreamType(Assessment assessment, AssessmentRequiredStreamType requiredStreamType) {
        return assessment.getRequiredStreamType().equals(requiredStreamType) || (assessment.getRequiredStreamType().equals(AssessmentRequiredStreamType.CAMERA_AND_SCREEN) && (assessment.getStreamTypePermission() == null || assessment.getStreamTypePermission().equals(AssessmentStreamTypePermission.ALL)));
    }
}
