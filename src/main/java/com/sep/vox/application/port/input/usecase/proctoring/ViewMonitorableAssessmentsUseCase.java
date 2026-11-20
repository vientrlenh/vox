package com.sep.vox.application.port.input.usecase.proctoring;

import java.time.Duration;
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
 * The assessments that are running or about to run that the logged-in user can proctor.
 *
 * <p>Replaces calling {@code assessments} and filtering on the client. The old path had two flaws: it required the proctor
 * to be a council member to see the assessment (but a council is not mandatory), and it took one page of the management list
 * and then filtered "running" in the browser -- so an assessment room could vanish from the proctoring screen just because the first page was full.
 */
@Service
public class ViewMonitorableAssessmentsUseCase implements IUseCase<Void, List<MonitoredAssessmentSummary>> {

    /**
     * Hiện trước giờ thi bao lâu.
     *
     * <p>Giám thị vào phòng trước khi ca bắt đầu -- để điểm danh, để chờ học viên kết nối. Danh sách
     * chỉ hiện đúng ca đang chạy thì đến lúc mở được cũng là lúc đã muộn. Nửa tiếng đủ rộng cho việc
     * chuẩn bị mà chưa kéo theo ca của buổi khác trong ngày.
     */
    static final Duration LEAD_TIME = Duration.ofMinutes(30);

    private final MonitoredAssessmentQueryRepository monitoredAssessmentQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMonitorableAssessmentsUseCase(
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
    public List<MonitoredAssessmentSummary> execute(Void input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Instant now = Instant.now();
        Instant leadUntil = now.plus(LEAD_TIME);

        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (isSchoolAdmin) {
            UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
            return monitoredAssessmentQueryRepository.findMonitorableBySchool(schoolId, null, now, leadUntil);
        }
        return monitoredAssessmentQueryRepository.findMonitorableByTeacher(currentUserId, null, now, leadUntil);
    }
}
