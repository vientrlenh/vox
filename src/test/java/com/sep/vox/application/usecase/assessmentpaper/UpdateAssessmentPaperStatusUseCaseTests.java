package com.sep.vox.application.usecase.assessmentpaper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.UpdateAssessmentPaperStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Kỳ thi tập trung có hai đường khoá mã đề, chọn theo ai soạn ra nó.
 *
 * <p>Mã đề do người khác soạn vẫn phải qua đủ DRAFT → IN_REVIEW → APPROVED → LOCKED, nên
 * {@code APPROVED} giữ nguyên nghĩa "đã qua mắt người thứ hai". Mã đề do chính người quyết định soạn
 * thì đi tắt một bước DRAFT → LOCKED: bắt họ tự nộp duyệt rồi tự duyệt chỉ tạo ra một dấu APPROVED
 * giả, còn cấm hẳn thì trường ít người không khoá nổi mã đề để phân đề.
 */
class UpdateAssessmentPaperStatusUseCaseTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID OTHER_AUTHOR_ID = UUID.randomUUID();

    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentPaperAutoAssigner assessmentPaperAutoAssigner;
    private UpdateAssessmentPaperStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentPaperAutoAssigner = mock(AssessmentPaperAutoAssigner.class);

        useCase = new UpdateAssessmentPaperStatusUseCase(
            assessmentPaperRepository,
            assessmentPaperItemRepository,
            assessmentRepository,
            assessmentCandidateRepository,
            new AssessmentPaperAuthoringAccessService(
                assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository),
            mock(AssessmentTimeQuotaGuardService.class),
            assessmentPaperAutoAssigner,
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(centralizedAssessment()));
        when(assessmentPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(false);
        when(assessmentPaperRepository.save(any(AssessmentPaper.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of());
        givenCallerRole(AssessmentMemberRole.CHAIR);
    }

    // --- Đường tắt: người quyết định tự soạn mã đề ---

    @Test
    void should_let_the_chair_lock_their_own_paper_in_one_step() {
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.LOCKED);
    }

    @Test
    void should_let_the_school_admin_lock_their_own_paper_in_one_step() {
        givenCallerIsSchoolAdminWithoutAssessmentRole();
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.LOCKED);
    }

    @Test
    void should_reject_the_one_step_lock_when_slots_are_still_empty() {
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);
        when(assessmentPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("còn ô câu hỏi chưa được gán");
        verify(assessmentPaperRepository, never()).save(any());
    }

    /**
     * After taking the shortcut it must be possible to come back and edit, otherwise the author
     * locks themselves out.
     */
    @Test
    void should_let_the_chair_reopen_their_own_locked_paper() {
        givenPaper(AssessmentPaperStatus.LOCKED, CALLER_ID);

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "REOPEN", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.DRAFT);
    }

    // --- Khoá mã đề cuối cùng là lúc phân đề tự động chạy được ---

    /**
     * Xếp thí sinh vào ca thường làm trước khi mã đề được khoá, nên gán đề lúc đó không chạy. Khoá mã
     * đề là thời điểm điều kiện vừa đủ -- gán bù ở đây, nếu không thí sinh kẹt "chưa có đề".
     */
    @Test
    void should_backfill_papers_after_locking() {
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null));

        verify(assessmentPaperAutoAssigner).backfillAssessment(any(Assessment.class), any(Instant.class), eq(CALLER_ID));
    }

    @Test
    void should_not_backfill_papers_when_only_submitting_for_review() {
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "SUBMIT", null));

        verify(assessmentPaperAutoAssigner, never()).backfillAssessment(any(), any(), any());
    }

    /**
     * Unlocking is for editing the variant again, and a candidate who is pointing at it would have
     * the assessment changed under their feet. Return them to "no paper assigned", exactly as when a
     * variant is deleted (DeleteAssessmentPaperUseCase); once it is locked again they can be assigned
     * again.
     */
    @Test
    void should_unassign_candidates_when_reopening_a_locked_paper() {
        givenPaper(AssessmentPaperStatus.LOCKED, CALLER_ID);
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(ASSESSMENT_ID);
        candidate.setAssignedPaperId(PAPER_ID);
        when(assessmentCandidateRepository.findByAssignedPaperId(PAPER_ID)).thenReturn(List.of(candidate));

        useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "REOPEN", null));

        assertThat(candidate.getAssignedPaperId()).isNull();
        verify(assessmentCandidateRepository).saveAll(List.of(candidate));
    }

    @Test
    void should_not_touch_candidates_when_reopen_is_rejected() {
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "REOPEN", null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(assessmentCandidateRepository, never()).saveAll(any());
    }

    // --- Đường đủ ba bước: mã đề do người khác soạn ---

    @Test
    void should_reject_locking_someone_elses_draft_paper_without_review() {
        givenPaper(AssessmentPaperStatus.DRAFT, OTHER_AUTHOR_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái đề thi hiện tại không hợp lệ");
        verify(assessmentPaperRepository, never()).save(any());
    }

    @Test
    void should_let_the_chair_lock_someone_elses_approved_paper() {
        givenPaper(AssessmentPaperStatus.APPROVED, OTHER_AUTHOR_ID);

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.LOCKED);
    }

    @Test
    void should_let_the_chair_approve_someone_elses_paper() {
        givenPaper(AssessmentPaperStatus.IN_REVIEW, OTHER_AUTHOR_ID);

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "APPROVE", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.APPROVED);
    }

    /** Chốt hồi quy cho maker-checker: đường tắt chỉ nới nhánh LOCK, không nới nhánh duyệt. */
    @Test
    void should_still_reject_approving_ones_own_paper() {
        givenPaper(AssessmentPaperStatus.IN_REVIEW, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "APPROVE", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Không được tự duyệt đề thi do chính mình tạo");
    }

    @Test
    void should_still_reject_the_school_admin_approving_their_own_paper() {
        givenCallerIsSchoolAdminWithoutAssessmentRole();
        givenPaper(AssessmentPaperStatus.IN_REVIEW, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "APPROVE", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Không được tự duyệt");
    }

    // --- Ranh giới vai trò ---

    @Test
    void should_let_an_author_submit_their_paper_for_review() {
        givenCallerRole(AssessmentMemberRole.AUTHOR);
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "SUBMIT", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.IN_REVIEW);
    }

    @Test
    void should_reject_an_author_locking_their_own_paper() {
        givenCallerRole(AssessmentMemberRole.AUTHOR);
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
    }

    @Test
    void should_reject_a_reviewer_locking_a_paper() {
        givenCallerRole(AssessmentMemberRole.REVIEWER);
        givenPaper(AssessmentPaperStatus.APPROVED, OTHER_AUTHOR_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_someone_with_no_role_on_the_assessment() {
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, CALLER_ID)).thenReturn(Optional.empty());
        givenPaper(AssessmentPaperStatus.APPROVED, OTHER_AUTHOR_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class);
    }

    /** A school admin from another school has no rights at all on this assessment. */
    @Test
    void should_reject_a_school_admin_from_another_school() {
        SchoolUser otherSchool = mock(SchoolUser.class);
        when(otherSchool.getSchoolId()).thenReturn(UUID.randomUUID());
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(otherSchool));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, CALLER_ID)).thenReturn(Optional.empty());
        givenPaper(AssessmentPaperStatus.DRAFT, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class);
    }

    private void givenCallerRole(AssessmentMemberRole role) {
        AssessmentMember member = AssessmentMember.builder()
                .assessmentId(ASSESSMENT_ID)
                .userId(CALLER_ID)
                .role(role)
                .grantedAt(Instant.now())
                .grantedBy(CALLER_ID)
                .build();
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, CALLER_ID)).thenReturn(Optional.of(member));
    }

    private void givenCallerIsSchoolAdminWithoutAssessmentRole() {
        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, CALLER_ID)).thenReturn(Optional.empty());
    }

    private void givenPaper(AssessmentPaperStatus status, UUID createdBy) {
        AssessmentPaper paper = new AssessmentPaper();
        paper.setId(PAPER_ID);
        paper.setAssessmentId(ASSESSMENT_ID);
        paper.setStatus(status);
        paper.setCreatedBy(createdBy.toString());
        when(assessmentPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
    }

    private Assessment centralizedAssessment() {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setSchoolId(SCHOOL_ID);
        assessment.setKind(AssessmentKind.CENTRALIZED);
        return assessment;
    }
}
