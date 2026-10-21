package com.sep.vox.application.port.input.usecase.stream;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.IssueMonitorTokenCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;


@Service
public class IssueMonitorTokenUseCase implements IUseCase<IssueMonitorTokenCommand, String>{

    private static final Duration MONITOR_TOKEN_TTL = Duration.ofMinutes(5);
    private static final long CLOCK_SKEW_SECONDS = 30;

    private static final String SCOPE_SCHOOL_ADMIN = "SCHOOL_ADMIN";
    private static final String SCOPE_ASSESSMENT_CHAIR = "ASSESSMENT_CHAIR";
    private static final String SCOPE_SCHEDULE_PROCTOR = "SCHEDULE_PROCTOR";

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserContextPort userContextPort;
    private final StreamTokenProvider streamTokenProvider;

    public IssueMonitorTokenUseCase(
        AssessmentRepository assessmentRepository,  
        AssessmentScheduleRepository assessmentScheduleRepository, 
        AssessmentScheduleProctorRepository assessmentScheduleProctorRepository, 
        AssessmentMemberRepository assessmentMemberRepository, 
        UserContextPort userContextPort, 
        StreamTokenProvider streamTokenProvider 
    ) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository; 
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.userContextPort = userContextPort; 
        this.streamTokenProvider = streamTokenProvider;
    }

    @Override
    public String execute(IssueMonitorTokenCommand input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        UUID schoolId = userContextPort.getCurrentSchoolId().orElse(null);
        var now = Instant.now();

        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi"));

        if (schoolId == null || !assessment.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền giám sát kỳ thi của trường được yêu cầu");
        }

        if (assessment.getRequiredStreamType() == null) {
            throw new IllegalArgumentException("Kỳ thi không hỗ trợ chức năng giám sát stream");
        }

        var activeSchedules = assessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessment.getId(), now);

        if (activeSchedules.isEmpty()) {
            throw new ResourceNotFoundException("Kỳ thi đang không có ca thi diễn ra");
        }

        var requestedIds = normalizeScheduleIds(input.scheduleIds());

        var activeScheduleByIds = activeSchedules.stream()
            .collect(Collectors.toMap(s -> s.getId(), s -> s));

        List<AssessmentSchedule> selectedSchedules;

        if (requestedIds.isEmpty()) {
            selectedSchedules = activeSchedules;
        } else {
            if (!activeScheduleByIds.keySet().containsAll(requestedIds)) {
                throw new IllegalArgumentException("Một hoặc nhiều ca thi không hợp lệ, không thuộc kỳ thi hoặc không đang diễn ra");
            }
            selectedSchedules = requestedIds.stream()
                .map(activeScheduleByIds::get)
                .toList();
        }

        String monitorScope;
        String role;

        if (userContextPort.isSchoolAdmin()) {
            monitorScope = SCOPE_SCHOOL_ADMIN;
            role = "SCHOOL_ADMIN";
        } else if (userContextPort.isTeacher()) {
            role = "TEACHER";

            var isChair = assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), userId, AssessmentMemberRole.CHAIR);

            if (isChair) {
                monitorScope = SCOPE_ASSESSMENT_CHAIR;
            } else {
                monitorScope = SCOPE_SCHEDULE_PROCTOR;

                selectedSchedules = restrictToProctoredSchedules(selectedSchedules, requestedIds, userId);
            }
        } else {
            throw new ForbiddenException("Vai trò hiện tại không được phép giám sát thi");
        }

        if (selectedSchedules.isEmpty()) {
            throw new ForbiddenException("Bạn không được phân công vào ca thi đang diễn ra nào");
        }

        var scheduleIds = selectedSchedules.stream()
            .map(s -> s.getId().toString())
            .distinct()
            .toList();

        var tokenExpiry = selectedSchedules.stream()
            .map(s -> s.getEndDate())
            .max(Comparator.naturalOrder())
            .orElseThrow();

        var ttlExpiry = now.plus(MONITOR_TOKEN_TTL);
        if (tokenExpiry.isAfter(ttlExpiry)) {
            tokenExpiry = ttlExpiry;
        }

        return streamTokenProvider.generateMonitorToken(
            userId.toString(), 
            schoolId.toString(), 
            assessment.getId().toString(), 
            monitorScope, 
            scheduleIds, 
            List.of(role), 
            now.minusSeconds(CLOCK_SKEW_SECONDS), 
            tokenExpiry
        );
    }

    private List<AssessmentSchedule> restrictToProctoredSchedules(List<AssessmentSchedule> schedules, List<UUID> explicityRequestedIds, UUID teacherId) {
        var scheduleIds = schedules.stream().map(s -> s.getId()).toList();
        var proctoredIds = new HashSet<>(assessmentScheduleProctorRepository.findScheduleIdsByTeacherIdAndScheduleIdIn(teacherId, scheduleIds));
        if (!explicityRequestedIds.isEmpty() && !proctoredIds.containsAll(explicityRequestedIds)) {
            throw new ForbiddenException("Bạn không được phân công vào một hoặc nhiều ca thi được yêu cầu");
        }
        return schedules.stream().filter(s -> proctoredIds.contains(s.getId())).toList();
    }

    private List<UUID> normalizeScheduleIds(List<UUID> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return List.of();
        }
        return scheduleIds.stream()
            .distinct()
            .toList();
    }
}
