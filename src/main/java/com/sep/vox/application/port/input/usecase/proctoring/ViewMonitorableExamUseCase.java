package com.sep.vox.application.port.input.usecase.proctoring;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.MonitoredExamSummary;
import com.sep.vox.application.query.repository.MonitoredExamQueryRepository;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Một kỳ thi, đọc bằng quyền GIÁM SÁT -- đủ để đặt tên cho màn danh sách ca thi.
 *
 * <p>Tồn tại riêng thay vì dùng {@code exam(id)} vì hai câu hỏi khác nhau: {@code exam(id)} là cửa
 * vào màn quản lý kỳ thi (thành viên hội đồng và nhà trường), còn đây chỉ trả tên/mã/loại cho phần
 * đầu trang giám sát. Nới cái trước để phục vụ cái sau là cách giám thị vào được cả dashboard.
 *
 * <p>KHÔNG lọc theo cửa sổ thời gian: mở lại một ca đã kết thúc để xem lại bằng chứng vẫn phải thấy
 * tên kỳ thi, nếu không thì đầu trang trống trong khi danh sách ca bên dưới vẫn liệt kê bình thường.
 */
@Service
public class ViewMonitorableExamUseCase implements IUseCase<UUID, MonitoredExamSummary> {

    private final MonitoredExamQueryRepository monitoredExamQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMonitorableExamUseCase(
            MonitoredExamQueryRepository monitoredExamQueryRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.monitoredExamQueryRepository = monitoredExamQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoredExamSummary execute(UUID examId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var now = Instant.now();

        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        var found = isSchoolAdmin
            ? monitoredExamQueryRepository.findMonitorableBySchool(requireSchoolId(currentUserId), examId, now, null)
            : monitoredExamQueryRepository.findMonitorableByTeacher(currentUserId, examId, now, null);

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
