package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Who may touch the paper variants of an assessment, and at what level.
 *
 * <p>This rule used to be scattered as a repeated line in the four paper authoring use cases
 * ({@code kind == CLASS_TEST ? CHAIR : AUTHOR}) and as a trio of separate functions in
 * {@code UpdateAssessmentPaperStatusUseCase}. It is gathered here because both groups now need the same answer:
 * <b>school administrators and the council chair can both author papers themselves</b>, not only the paper author.
 *
 * <p>The role is read with exactly one query {@code findByAssessmentIdAndUserId} instead of three calls to
 * {@code existsByAssessmentIdAndUserIdAndRole} as before.
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
        AssessmentMemberRole role = assessmentMemberRepository.findByAssessmentIdAndUserId(assessment.getId(), userId)
            .map(member -> member.getRole())
            .orElse(null);
        // A class test is the business of its owner teacher, and school administrators do not coordinate it (the same boundary
        // that AssessmentGradingAccessService builds for grading). Blocked right here so that every use case using this service
        // does not have to remember to exclude it itself.
        boolean schoolAdmin = assessment.getKind() != AssessmentKind.CLASS_TEST && isSchoolAdminOfAssessmentSchool(assessment, userId);
        return new PaperActor(
            schoolAdmin,
            role == AssessmentMemberRole.CHAIR,
            role == AssessmentMemberRole.AUTHOR,
            role == AssessmentMemberRole.REVIEWER
        );
    }

    /** Ném {@link ForbiddenException} nếu người gọi không được soạn nội dung mã đề của kỳ thi này. */
    public PaperActor requireCanAuthor(Assessment assessment, UUID userId) {
        PaperActor actor = resolve(assessment, userId);
        if (!actor.canAuthor()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return actor;
    }

    private boolean isSchoolAdminOfAssessmentSchool(Assessment assessment, UUID userId) {
        UUID currentSchoolId = schoolUserRepository.findByUserId(userId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (currentSchoolId == null || !currentSchoolId.equals(assessment.getSchoolId())) {
            return false;
        }
        return userRoleQueryRepository.findByUserIdWithRoleInfo(userId).stream()
            .anyMatch(role -> SCHOOL_ADMIN.equals(role.roleCode()));
    }
}
