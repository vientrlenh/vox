package com.sep.vox.application.port.input.usecase.exam;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewExamDetailsUseCase implements IUseCase<ViewExamDetailsQuery, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewExamDetailsUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDto execute(ViewExamDetailsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !userContextPort.isSystemAdmin()
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (!hasAccess(exam.getId(), exam.getSchoolId(), exam.getStatus(), currentUserId, currentSchoolId, schoolAdmin)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        return ExamDtoMapper.toDto(exam);
    }

    private boolean hasAccess(
            UUID examId,
            UUID examSchoolId,
            ExamStatus examStatus,
            UUID currentUserId,
            UUID currentSchoolId,
            boolean schoolAdmin) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(examSchoolId)) {
            return true;
        }
        // Sau khi kỳ thi đã đóng, ai cùng trường cũng xem được — không cần là member nữa.
        if ((examStatus == ExamStatus.CLOSED || examStatus == ExamStatus.RESULTS_PUBLISHED)
                && currentSchoolId != null && currentSchoolId.equals(examSchoolId)) {
            return true;
        }
        // Giám thị được phân công ca thi CỐ Ý không có mặt ở đây, dù họ có quan hệ thật với kỳ thi.
        //
        // Đây là cửa vào màn quản lý kỳ thi, nên mở nó cho giám thị là đưa họ thẳng vào dashboard --
        // chỗ của hội đồng và nhà trường. Nhu cầu "giám thị đọc được tên kỳ thi mình gác" có đường
        // riêng: ViewMonitorableExamUseCase, trả đúng vài trường mà đầu trang giám sát cần.
        return examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR)
            || examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.AUTHOR)
            || examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.REVIEWER);
    }
}
