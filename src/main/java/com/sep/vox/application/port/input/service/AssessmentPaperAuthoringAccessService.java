package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;

/**
 * Ai được đụng vào mã đề của một kỳ thi, và đụng ở mức nào.
 *
 * <p>Luật này trước đây nằm rải rác dưới dạng một dòng lặp lại ở bốn use case soạn đề
 * ({@code kind == CLASS_TEST ? CHAIR : AUTHOR}) và một bộ ba hàm riêng ở
 * {@code UpdateAssessmentPaperStatusUseCase}. Gom về đây vì cả hai nhóm giờ cần cùng một câu trả lời:
 * <b>quản trị trường và chủ tịch hội đồng đều có thể tự soạn đề</b>, không chỉ người ra đề.
 *
 * <p>Đọc vai trò bằng đúng một truy vấn {@code findByAssessmentIdAndUserId} thay vì ba lần
 * {@code existsByAssessmentIdAndUserIdAndRole} như trước.
 */
@Service
public class AssessmentPaperAuthoringAccessService {

    private static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";

    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public AssessmentPaperAuthoringAccessService(
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    /**
     * @param schoolAdmin người gọi là quản trị trường <b>của chính trường kỳ thi</b> — quản trị trường
     *                    khác trường không có quyền gì ở đây.
     */
    public record PaperActor(boolean schoolAdmin, boolean chair, boolean author, boolean reviewer) {

        /** Tạo mã đề, gán câu hỏi, sửa section, xoá mã đề. */
        public boolean canAuthor() {
            return schoolAdmin || chair || author;
        }

        /** Duyệt / trả về sửa lại. Chủ tịch có toàn quyền của người duyệt. */
        public boolean canReview() {
            return schoolAdmin || chair || reviewer;
        }

        /** Khoá / mở lại mã đề — bước quyết định cuối, không mở cho người ra đề lẫn người duyệt. */
        public boolean canDecide() {
            return schoolAdmin || chair;
        }
    }

    public PaperActor resolve(Assessment assessment, UUID userId) {
        var role = assessmentMemberRepository.findByAssessmentIdAndUserId(assessment.getId(), userId)
            .map(member -> member.getRole())
            .orElse(null);
        // Bài trên lớp là việc của giáo viên chủ bài, quản trị trường không điều phối (cùng ranh giới
        // mà AssessmentGradingAccessService dựng cho khâu chấm). Chặn ngay ở đây để mọi use case dùng service
        // này khỏi phải nhớ tự loại trừ.
        var schoolAdmin = assessment.getKind() != AssessmentKind.CLASS_TEST && isSchoolAdminOfAssessmentSchool(assessment, userId);
        return new PaperActor(
            schoolAdmin,
            role == AssessmentMemberRole.CHAIR,
            role == AssessmentMemberRole.AUTHOR,
            role == AssessmentMemberRole.REVIEWER
        );
    }

    /** Ném {@link ForbiddenException} nếu người gọi không được soạn nội dung mã đề của kỳ thi này. */
    public PaperActor requireCanAuthor(Assessment assessment, UUID userId) {
        var actor = resolve(assessment, userId);
        if (!actor.canAuthor()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return actor;
    }

    private boolean isSchoolAdminOfAssessmentSchool(Assessment assessment, UUID userId) {
        var currentSchoolId = schoolUserRepository.findByUserId(userId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (currentSchoolId == null || !currentSchoolId.equals(assessment.getSchoolId())) {
            return false;
        }
        return userRoleQueryRepository.findByUserIdWithRoleInfo(userId).stream()
            .anyMatch(role -> SCHOOL_ADMIN.equals(role.roleCode()));
    }
}
