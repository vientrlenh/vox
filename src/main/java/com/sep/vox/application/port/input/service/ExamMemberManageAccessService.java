package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Ai được lập hội đồng đề của một kỳ thi tập trung, và lập tới đâu.
 *
 * <p>Quản trị trường của chính trường đó, hoặc chủ tịch hội đồng của chính kỳ thi đó — chủ tịch cần
 * quyền này vì họ là người chạy trọn quy trình trên trang chi tiết kỳ thi, mà bước đầu tiên chính là
 * gọi người ra đề và người duyệt đề vào hội đồng.
 *
 * <p>Ranh giới duy nhất giữa hai vai: <b>chỉ quản trị trường mới đụng được vào hàng CHAIR</b>. Chủ
 * tịch tự thu hồi vai trò của mình (hoặc của chủ tịch khác) là kỳ thi mất người quyết định mà không
 * ai trong luồng giáo viên dựng lại được — bổ nhiệm chủ tịch phải là việc của quản trị trường.
 *
 * <p>Hội đồng chỉ tồn tại ở kỳ thi tập trung; bài trên lớp có đúng một CHAIR do
 * {@code CreateClassTestUseCase} tự gán nên không đi qua đây.
 */
@Service
public class ExamMemberManageAccessService {

    private static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";

    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ExamMemberManageAccessService(
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * @param schoolAdmin true khi người gọi là quản trị trường của chính trường kỳ thi; false nghĩa là
     *                    họ vào đây với tư cách chủ tịch hội đồng.
     */
    public record ExamMemberActor(UUID userId, boolean schoolAdmin) {
    }

    /** Ném {@link ForbiddenException} nếu người gọi không được lập hội đồng cho kỳ thi này. */
    public ExamMemberActor requireCanManage(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (exam.getKind() != ExamKind.CENTRALIZED) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> SCHOOL_ADMIN.equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return new ExamMemberActor(currentUserId, true);
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return new ExamMemberActor(currentUserId, false);
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    /**
     * Gọi cho <b>mọi</b> vai trò mà thao tác chạm tới. Đổi vai trò của một thành viên chạm tới hai vai:
     * vai cũ (bị gỡ) và vai mới (được gán) — bỏ sót vai cũ là chủ tịch hạ cấp được chủ tịch khác.
     */
    public void requireCanTouchRole(ExamMemberActor actor, ExamMemberRole targetRole) {
        if (targetRole == ExamMemberRole.CHAIR && !actor.schoolAdmin()) {
            throw new ForbiddenException("Chỉ quản trị trường mới bổ nhiệm hoặc thu hồi chủ tịch hội đồng");
        }
    }
}
