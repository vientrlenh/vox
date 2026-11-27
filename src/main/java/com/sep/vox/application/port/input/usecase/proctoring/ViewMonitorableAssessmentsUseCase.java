package com.sep.vox.application.port.input.usecase.proctoring;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.MonitoredAssessmentSummary;
import com.sep.vox.application.query.repository.MonitoredAssessmentQueryRepository;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * The assessments that are running or about to run that the logged-in user can proctor.
 *
 * <p>Replaces calling {@code assessments} and filtering on the client. The old path had two flaws: it required the proctor
 * to be a council member to see the assessment (but a council is not mandatory), and it took one page of the management list
 * and then filtered "running" in the browser -- so an assessment room could vanish from the proctoring screen just because the first page was full.
 */
@Service
@RequiredArgsConstructor
public class ViewMonitorableAssessmentsUseCase implements IUseCase<Void, List<MonitoredAssessmentSummary>> {

    /**
     * How long before the exam time it is shown.
     *
     * <p>The proctor enters the room before the session starts -- to take attendance and wait for students to connect. If the list
     * showed only the running session, it would be too late by the time it could be opened. Half an hour is wide enough for
     * preparation without pulling in sessions of another period of the day.
     */
    static final Duration LEAD_TIME = Duration.ofMinutes(30);

    private final MonitoredAssessmentQueryRepository monitoredAssessmentQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<MonitoredAssessmentSummary> execute(Void input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Instant now = Instant.now();
        Instant leadUntil = now.plus(LEAD_TIME);

        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (isSchoolAdmin) {
            UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Access denied"));
            return monitoredAssessmentQueryRepository.findMonitorableBySchool(schoolId, null, now, leadUntil);
        }
        return monitoredAssessmentQueryRepository.findMonitorableByTeacher(currentUserId, null, now, leadUntil);
    }
}
