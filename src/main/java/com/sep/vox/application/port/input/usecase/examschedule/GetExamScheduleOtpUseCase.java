package com.sep.vox.application.port.input.usecase.examschedule;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.GetExamScheduleOtpQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.examschedule.GetExamScheduleOtpResponse;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

@Service
public class GetExamScheduleOtpUseCase implements IUseCase<GetExamScheduleOtpQuery, GetExamScheduleOtpResponse> {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final CacheManagerPort cacheManagerPort;
    private final UserContextPort userContextPort;

    public GetExamScheduleOtpUseCase(ExamScheduleRepository examScheduleRepository, ExamScheduleProctorRepository examScheduleProctorRepository, OneTimePasswordPort oneTimePasswordPort, CacheManagerPort cacheManagerPort, UserContextPort userContextPort) {
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.oneTimePasswordPort = oneTimePasswordPort;
        this.cacheManagerPort = cacheManagerPort;
        this.userContextPort = userContextPort;
    }

    private static final int OTP_LENGTH = 8;
    private static final Duration TTL = Duration.ofSeconds(60);

    @Override
    public GetExamScheduleOtpResponse execute(GetExamScheduleOtpQuery input) {
        var now = OffsetDateTime.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var schedule = examScheduleRepository.findByIdAndInSchedule(input.scheduleId(), now)
            .orElseThrow(() -> new NotFoundException("Lịch thi yêu cầu không tìm thấy hoặc đã hết hạn"));

        if (!schedule.getExamId().equals(input.examId())) {
            throw new IllegalArgumentException("Lịch thi yêu cầu không thuộc về kỳ thi này");
        }
        if (!examScheduleProctorRepository.existsByScheduleIdAndTeacherId(input.scheduleId(), userId)) {
            throw new ForbiddenException("Lịch thi này không được bạn giám sát");
        }

        var otpKey = CacheKey.examScheduleOtpKey(input.scheduleId());
        var otp = oneTimePasswordPort.generate(OTP_LENGTH);
        otp = cacheManagerPort.saveIfAbsentAndGet(otpKey, otp, TTL);
        var expiresSeconds = cacheManagerPort.getRemainingTtl(otpKey);

        return new GetExamScheduleOtpResponse(otp, expiresSeconds);
    }
    
}
