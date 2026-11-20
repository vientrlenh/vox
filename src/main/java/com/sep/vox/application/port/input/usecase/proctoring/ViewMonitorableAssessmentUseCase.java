package com.sep.vox.application.port.input.usecase.proctoring;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
 * One assessment, read with PROCTORING permission -- enough to name the session list screen.
 *
 * <p>It exists separately instead of using {@code assessment(id)} because they answer two different questions: {@code assessment(id)} is the entry
 * to the assessment management screen (council members and the school), while this only returns the name/code/kind for the
 * top of the proctoring page. Loosening the former to serve the latter is how a proctor would get into the whole dashboard.
 *
 * <p>Does NOT filter by time window: reopening a session that has ended to review evidence must still show the
 * name of the assessment, otherwise the top of the page is empty while the session list below still lists normally.
 */
@Service
public class ViewMonitorableAssessmentUseCase implements IUseCase<UUID, MonitoredAssessmentSummary> {

    private final MonitoredAssessmentQueryRepository monitoredAssessmentQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMonitorableAssessmentUseCase(
            MonitoredAssessmentQueryRepository monitoredAssessmentQueryRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.monitoredAssessmentQueryRepository = monitoredAssessmentQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoredAssessmentSummary execute(UUID assessmentId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Instant now = Instant.now();

        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        List<MonitoredAssessmentSummary> found = isSchoolAdmin
            ? monitoredAssessmentQueryRepository.findMonitorableBySchool(requireSchoolId(currentUserId), assessmentId, now, null)
            : monitoredAssessmentQueryRepository.findMonitorableByTeacher(currentUserId, assessmentId, now, null);

        // Rỗng nghĩa là không gác ca nào của kỳ thi này -- chính là "không có quyền", nên trả 403 chứ
        // không phải 404: nói "không tìm thấy" ở đây sẽ khiến giám thị đi báo kỳ thi bị mất.
        return found.stream()
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("Bạn không giám sát ca thi nào của kỳ thi này"));
    }

    private UUID requireSchoolId(UUID currentUserId) {
        return schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
    }
}
