package com.sep.vox.application.port.input.usecase.stream;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.stream.IssueStudentStreamTokenCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.stream.IssueStudentStreamTokenResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;

@Service
@RequiredArgsConstructor
public class IssueStudentStreamTokenUseCase implements IUseCase<IssueStudentStreamTokenCommand, IssueStudentStreamTokenResponse> {

    private final UserContextPort userContextPort;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final StreamTokenProvider streamTokenProvider; 


    @Override
    @Transactional
    public IssueStudentStreamTokenResponse execute(IssueStudentStreamTokenCommand command) {
        Instant now = Instant.now();
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        AssessmentSession assessmentSession = assessmentSessionRepository.findByIdAndResumable(command.assessmentSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã hết hạn"));

        Assessment assessment = assessmentRepository.findById(assessmentSession.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        AssessmentCandidate candidate = findOwnCandidate(assessmentSession, userId);

        AssessmentSchedule schedule = assessmentScheduleRepository.findByIdAndInSchedule(candidate.getScheduleId(), now)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch thi"));

        // After the whole authentication step, not before: this step WRITES to the assessment session (it fixes
        // the stream type), so if it ran earlier, someone who does not own the session could still fix the
        // stream type on its behalf before being stopped by the permission check above.
        List<String> streamTypes = resolveStreamTypes(assessment, assessmentSession, command.streamType());

        String token = generateStreamToken(userId, candidate, schedule, assessment, assessmentSession, streamTypes);

        return new IssueStudentStreamTokenResponse(token, schedule.getId(), assessmentSession.getId(), streamTypes, schedule.getEndDate());
    }

    private AssessmentCandidate findOwnCandidate(AssessmentSession assessmentSession, UUID userId) {
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(assessmentSession.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên thi"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return candidate;
    }

    private String generateStreamToken(UUID userId, AssessmentCandidate candidate, AssessmentSchedule schedule,
            Assessment assessment, AssessmentSession assessmentSession, List<String> streamTypes) {
        return streamTokenProvider.generateStreamToken(
            userId.toString(),
            candidate.getId().toString(),
            schedule.getId().toString(),
            assessment.getId().toString(),
            assessmentSession.getId().toString(),
            streamTypes,
            schedule.getStartDate(),
            schedule.getEndDate()
        );
    }


    /**
     * Returns the stream types for the assessment session (lower case, the form vox-streaming
     * expects) and fixes the choice on the first call.
     *
     * <p>An assessment session issues tokens many times: on entry, on reconnect, and every time
     * UploadCredentialRefresher renews. If each call were recomputed from the request, a learner in
     * an assessment configured with {@code ANY} could start with camera + screen and then request
     * a token with camera only, switching the screen recording off midway without leaving a trace.
     * So the first call writes the choice to the database and later calls read it back.
     */
    private List<String> resolveStreamTypes(Assessment assessment, AssessmentSession assessmentSession, AssessmentRequiredStreamType requestedStreamType) {
        if (assessment.getRequiredStreamType() == null) {
            throw new IllegalArgumentException("Kỳ thi không hỗ trợ stream");
        }

        AssessmentRequiredStreamType chosenStreamType = assessmentSession.getChosenStreamType();
        if (chosenStreamType == null) {
            chosenStreamType = lockChosenStreamType(assessmentSession, resolveFirstChoice(assessment, requestedStreamType));
        }

        if (requestedStreamType != null && requestedStreamType != chosenStreamType) {
            throw new ForbiddenException(
                "Phiên thi đã chốt loại stream " + chosenStreamType.name() + ", không thể đổi giữa chừng"
            );
        }
        return toStreamTypes(chosenStreamType);
    }

    /**
     * Writes the choice to the assessment session. If a parallel request wins (0 rows updated), the winning value
     * is read back instead of being overwritten - the caller compares that value with the original request.
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
     * Stream type for the first token issued. Empty means "follow the assessment configuration":
     * the client does not need to know {@code requiredStreamType} to ask for a token.
     */
    private AssessmentRequiredStreamType resolveFirstChoice(Assessment assessment, AssessmentRequiredStreamType requestedStreamType) {
        if (requestedStreamType == null) {
            return assessment.getRequiredStreamType();
        }
        switch (requestedStreamType) {
            case CAMERA:
                if (isInvalidRequestedIndividualStreamType(assessment, AssessmentRequiredStreamType.SCREEN)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream screen");
                }
                return AssessmentRequiredStreamType.CAMERA;
            case SCREEN:
                if (isInvalidRequestedIndividualStreamType(assessment, AssessmentRequiredStreamType.CAMERA)) {
                    throw new IllegalArgumentException("Kỳ thi đang chỉ hỗ trợ stream camera");
                }
                return AssessmentRequiredStreamType.SCREEN;
            case CAMERA_AND_SCREEN:
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
