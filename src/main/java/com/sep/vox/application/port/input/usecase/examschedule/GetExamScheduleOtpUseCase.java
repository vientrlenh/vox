package com.sep.vox.application.port.input.usecase.examschedule;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.GetExamScheduleOtpQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.examschedule.GetExamScheduleOtpResponse;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class GetExamScheduleOtpUseCase implements IUseCase<GetExamScheduleOtpQuery, GetExamScheduleOtpResponse> {

    private static final int OTP_LENGTH = 8;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamRepository examRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final CacheManagerPort cacheManagerPort;
    private final UserContextPort userContextPort;

    public GetExamScheduleOtpUseCase(
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamRepository examRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            OneTimePasswordPort oneTimePasswordPort,
            CacheManagerPort cacheManagerPort,
            UserContextPort userContextPort) {
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examRepository = examRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.oneTimePasswordPort = oneTimePasswordPort;
        this.cacheManagerPort = cacheManagerPort;
        this.userContextPort = userContextPort;
    }

    @Override
    public GetExamScheduleOtpResponse execute(GetExamScheduleOtpQuery input) {
        var now = OffsetDateTime.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var schedule = examScheduleRepository.findByIdAndInSchedule(input.scheduleId(), now)
            .orElseThrow(() -> new NotFoundException("Lịch thi yêu cầu không tìm thấy hoặc đã hết hạn"));

        if (!schedule.getExamId().equals(input.examId())) {
            throw new IllegalArgumentException("Lịch thi yêu cầu không thuộc về kỳ thi này");
        }

        var exam = examRepository.findById(schedule.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của lịch thi này"));
        if (!hasAccess(input.scheduleId(), exam.getSchoolId(), userId)) {
            throw new ForbiddenException("Lịch thi này không được bạn giám sát");
        }

        var otpKey = CacheKey.examScheduleOtpKey(input.scheduleId());
        var otp = oneTimePasswordPort.generate(OTP_LENGTH);
        otp = cacheManagerPort.saveIfAbsentAndGet(otpKey, otp, TTL);
        var expiresSeconds = cacheManagerPort.getRemainingTtl(otpKey);

        return new GetExamScheduleOtpResponse(otp, expiresSeconds);
    }

    private boolean hasAccess(UUID scheduleId, UUID examSchoolId, UUID currentUserId) {
        if (examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(examSchoolId);
    }
}
