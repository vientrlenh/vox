package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.GetAssessmentScheduleOtpQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessmentschedule.GetAssessmentScheduleOtpResponse;
import com.sep.vox.application.shared.CacheKey;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

@Service
public class GetAssessmentScheduleOtpUseCase implements IUseCase<GetAssessmentScheduleOtpQuery, GetAssessmentScheduleOtpResponse> {

    private static final int OTP_LENGTH = 8;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentRepository assessmentRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final CacheManagerPort cacheManagerPort;
    private final UserContextPort userContextPort;

    public GetAssessmentScheduleOtpUseCase(
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentRepository assessmentRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            OneTimePasswordPort oneTimePasswordPort,
            CacheManagerPort cacheManagerPort,
            UserContextPort userContextPort) {
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentRepository = assessmentRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.oneTimePasswordPort = oneTimePasswordPort;
        this.cacheManagerPort = cacheManagerPort;
        this.userContextPort = userContextPort;
    }

    @Override
    public GetAssessmentScheduleOtpResponse execute(GetAssessmentScheduleOtpQuery input) {
        var now = Instant.now();
        UUID userId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var schedule = assessmentScheduleRepository.findByIdAndInSchedule(input.scheduleId(), now)
            .orElseThrow(() -> new ResourceNotFoundException("Lịch thi yêu cầu không tìm thấy hoặc đã hết hạn"));

        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new IllegalArgumentException("Lịch thi yêu cầu không thuộc về kỳ thi này");
        }

        var assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của lịch thi này"));
        if (!hasAccess(input.scheduleId(), assessment.getSchoolId(), userId)) {
            throw new ForbiddenException("Lịch thi này không được bạn giám sát");
        }

        var otpKey = CacheKey.assessmentScheduleOtpKey(input.scheduleId());
        var otp = oneTimePasswordPort.generate(OTP_LENGTH);
        otp = cacheManagerPort.saveIfAbsentAndGet(otpKey, otp, TTL);
        var expiresSeconds = cacheManagerPort.getRemainingTtl(otpKey);

        return new GetAssessmentScheduleOtpResponse(otp, expiresSeconds);
    }

    private boolean hasAccess(UUID scheduleId, UUID assessmentSchoolId, UUID currentUserId) {
        if (assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId);
    }
}
