package com.sep.vox.application.port.input.usecase.proctoring;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.MonitoredAssessmentSummary;
import com.sep.vox.application.projection.repository.MonitoredAssessmentQueryRepository;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Kỳ thi đang diễn ra hoặc sắp diễn ra mà người đang đăng nhập giám sát được.
 *
 * <p>Thay cho việc gọi {@code assessments} rồi tự lọc ở client. Đường cũ có hai chỗ hỏng: nó bắt giám thị
 * phải là thành viên hội đồng mới thấy được kỳ thi (mà hội đồng thì không bắt buộc), và nó lấy một
 * trang danh sách quản lý rồi lọc "đang diễn ra" ở trình duyệt -- nên một phòng thi có thể biến mất
 * khỏi màn giám sát chỉ vì trang đầu đã kín.
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
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var now = Instant.now();
        var leadUntil = now.plus(LEAD_TIME);

        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (isSchoolAdmin) {
            var schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
            return monitoredAssessmentQueryRepository.findMonitorableBySchool(schoolId, null, now, leadUntil);
        }
        return monitoredAssessmentQueryRepository.findMonitorableByTeacher(currentUserId, null, now, leadUntil);
    }
}
