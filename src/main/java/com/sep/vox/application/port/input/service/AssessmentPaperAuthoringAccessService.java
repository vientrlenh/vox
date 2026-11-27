package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AssessmentPaperAuthoringAccessService {

    private static final String SCHOOL_ADMIN = "SCHOOL_ADMIN";

    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    /**
     * @param schoolAdmin the caller is the school admin <b>of the assessment's own school</b> — a school admin
     *                    of another school has no rights here.
     */
    public record PaperActor(boolean schoolAdmin, boolean chair, boolean author, boolean reviewer) {

        /** Create a paper, assign questions, edit a section, delete a paper. */
        public boolean canAuthor() {
            return schoolAdmin || chair || author;
        }

        /** Approve / send back for rework. The chair has all the reviewer's rights. */
        public boolean canReview() {
            return schoolAdmin || chair || reviewer;
        }

        /** Lock / reopen a paper — the final deciding step, not open to the author or the reviewer. */
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

    /** Throws {@link ForbiddenException} if the caller may not author the paper content of this assessment. */
    public PaperActor requireCanAuthor(Assessment assessment, UUID userId) {
        PaperActor actor = resolve(assessment, userId);
        if (!actor.canAuthor()) {
            throw new ForbiddenException("Access denied");
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
