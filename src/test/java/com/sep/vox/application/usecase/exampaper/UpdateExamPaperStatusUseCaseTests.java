package com.sep.vox.application.usecase.exampaper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.sep.vox.application.port.input.command.UpdateExamPaperStatusCommand;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Kỳ thi tập trung có hai đường khoá mã đề, chọn theo ai soạn ra nó.
 *
 * <p>Mã đề do người khác soạn vẫn phải qua đủ DRAFT → IN_REVIEW → APPROVED → LOCKED, nên
 * {@code APPROVED} giữ nguyên nghĩa "đã qua mắt người thứ hai". Mã đề do chính người quyết định soạn
 * thì đi tắt một bước DRAFT → LOCKED: bắt họ tự nộp duyệt rồi tự duyệt chỉ tạo ra một dấu APPROVED
 * giả, còn cấm hẳn thì trường ít người không khoá nổi mã đề để phân đề.
 */
class UpdateExamPaperStatusUseCaseTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID OTHER_AUTHOR_ID = UUID.randomUUID();

    private ExamPaperRepository examPaperRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UpdateExamPaperStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        examPaperRepository = mock(ExamPaperRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        var examRepository = mock(ExamRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new UpdateExamPaperStatusUseCase(
            examPaperRepository,
            examPaperItemRepository,
            examRepository,
            new ExamPaperAuthoringAccessService(
                examMemberRepository, schoolUserRepository, userRoleQueryRepository),
            mock(ExamTimeQuotaGuardService.class),
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(centralizedExam()));
        when(examPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(false);
        when(examPaperRepository.save(any(ExamPaper.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of());
        givenCallerRole(ExamMemberRole.CHAIR);
    }

    // --- Đường tắt: người quyết định tự soạn mã đề ---

    @Test
    void should_let_the_chair_lock_their_own_paper_in_one_step() {
        givenPaper(ExamPaperStatus.DRAFT, CALLER_ID);

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.LOCKED.name());
    }

    @Test
    void should_let_the_school_admin_lock_their_own_paper_in_one_step() {
        givenCallerIsSchoolAdminWithoutExamRole();
        givenPaper(ExamPaperStatus.DRAFT, CALLER_ID);

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.LOCKED.name());
    }

    @Test
    void should_reject_the_one_step_lock_when_slots_are_still_empty() {
        givenPaper(ExamPaperStatus.DRAFT, CALLER_ID);
        when(examPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("còn ô câu hỏi chưa được gán");
        verify(examPaperRepository, never()).save(any());
    }

    /** Đi tắt rồi phải quay lại sửa được, nếu không người soạn tự khoá mình ra ngoài. */
    @Test
    void should_let_the_chair_reopen_their_own_locked_paper() {
        givenPaper(ExamPaperStatus.LOCKED, CALLER_ID);

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "REOPEN", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.DRAFT.name());
    }

    // --- Đường đủ ba bước: mã đề do người khác soạn ---

    @Test
    void should_reject_locking_someone_elses_draft_paper_without_review() {
        givenPaper(ExamPaperStatus.DRAFT, OTHER_AUTHOR_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Trạng thái đề thi hiện tại không hợp lệ");
        verify(examPaperRepository, never()).save(any());
    }

    @Test
    void should_let_the_chair_lock_someone_elses_approved_paper() {
        givenPaper(ExamPaperStatus.APPROVED, OTHER_AUTHOR_ID);

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.LOCKED.name());
    }

    @Test
    void should_let_the_chair_approve_someone_elses_paper() {
        givenPaper(ExamPaperStatus.IN_REVIEW, OTHER_AUTHOR_ID);

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "APPROVE", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.APPROVED.name());
    }

    /** Chốt hồi quy cho maker-checker: đường tắt chỉ nới nhánh LOCK, không nới nhánh duyệt. */
    @Test
    void should_still_reject_approving_ones_own_paper() {
        givenPaper(ExamPaperStatus.IN_REVIEW, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "APPROVE", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Không được tự duyệt đề thi do chính mình tạo");
    }

    @Test
    void should_still_reject_the_school_admin_approving_their_own_paper() {
        givenCallerIsSchoolAdminWithoutExamRole();
        givenPaper(ExamPaperStatus.IN_REVIEW, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "APPROVE", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Không được tự duyệt");
    }

    // --- Ranh giới vai trò ---

    @Test
    void should_let_an_author_submit_their_paper_for_review() {
        givenCallerRole(ExamMemberRole.AUTHOR);
        givenPaper(ExamPaperStatus.DRAFT, CALLER_ID);

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "SUBMIT", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.IN_REVIEW.name());
    }

    @Test
    void should_reject_an_author_locking_their_own_paper() {
        givenCallerRole(ExamMemberRole.AUTHOR);
        givenPaper(ExamPaperStatus.DRAFT, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
    }

    @Test
    void should_reject_a_reviewer_locking_a_paper() {
        givenCallerRole(ExamMemberRole.REVIEWER);
        givenPaper(ExamPaperStatus.APPROVED, OTHER_AUTHOR_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_someone_with_no_role_on_the_exam() {
        when(examMemberRepository.findByExamIdAndUserId(EXAM_ID, CALLER_ID)).thenReturn(Optional.empty());
        givenPaper(ExamPaperStatus.APPROVED, OTHER_AUTHOR_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class);
    }

    /** Quản trị trường khác trường không có quyền gì trên kỳ thi này. */
    @Test
    void should_reject_a_school_admin_from_another_school() {
        var otherSchool = mock(SchoolUser.class);
        when(otherSchool.getSchoolId()).thenReturn(UUID.randomUUID());
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(otherSchool));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
        when(examMemberRepository.findByExamIdAndUserId(EXAM_ID, CALLER_ID)).thenReturn(Optional.empty());
        givenPaper(ExamPaperStatus.DRAFT, CALLER_ID);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class);
    }

    private void givenCallerRole(ExamMemberRole role) {
        var member = new ExamMember(EXAM_ID, CALLER_ID, role, Instant.now(), CALLER_ID);
        when(examMemberRepository.findByExamIdAndUserId(EXAM_ID, CALLER_ID)).thenReturn(Optional.of(member));
    }

    private void givenCallerIsSchoolAdminWithoutExamRole() {
        var schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
        when(examMemberRepository.findByExamIdAndUserId(EXAM_ID, CALLER_ID)).thenReturn(Optional.empty());
    }

    private void givenPaper(ExamPaperStatus status, UUID createdBy) {
        var paper = new ExamPaper();
        paper.setId(PAPER_ID);
        paper.setExamId(EXAM_ID);
        paper.setStatus(status);
        paper.setCreatedBy(createdBy);
        when(examPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
    }

    private Exam centralizedExam() {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setSchoolId(SCHOOL_ID);
        exam.setKind(ExamKind.CENTRALIZED);
        return exam;
    }
}
