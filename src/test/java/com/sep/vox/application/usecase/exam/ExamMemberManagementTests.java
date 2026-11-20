package com.sep.vox.application.usecase.exam;

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
import com.sep.vox.application.port.input.command.CreateExamMemberCommand;
import com.sep.vox.application.port.input.command.DeleteExamMemberCommand;
import com.sep.vox.application.port.input.command.UpdateExamMemberCommand;
import com.sep.vox.application.port.input.service.ExamMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.exam.CreateExamMemberUseCase;
import com.sep.vox.application.port.input.usecase.exam.DeleteExamMemberUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamMemberUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Chủ tịch hội đồng tự lập hội đồng ra đề của kỳ thi mình chủ trì — trước đây ba endpoint này chỉ
 * nhận quản trị trường, nên giáo viên chủ tịch không qua nổi bước đầu tiên của quy trình.
 *
 * <p>Ranh giới còn lại: hàng CHAIR vẫn chỉ quản trị trường đụng được. Chủ tịch thu hồi vai trò của
 * chính mình hay của chủ tịch khác là kỳ thi mất người quyết định mà luồng giáo viên không dựng lại
 * được.
 */
class ExamMemberManagementTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;

    private CreateExamMemberUseCase createUseCase;
    private UpdateExamMemberUseCase updateUseCase;
    private DeleteExamMemberUseCase deleteUseCase;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        var userContextPort = mock(UserContextPort.class);

        var accessService = new ExamMemberManageAccessService(
            examMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);
        createUseCase = new CreateExamMemberUseCase(
            examRepository, examMemberRepository, schoolUserRepository, accessService);
        updateUseCase = new UpdateExamMemberUseCase(examRepository, examMemberRepository, accessService);
        deleteUseCase = new DeleteExamMemberUseCase(examRepository, examMemberRepository, accessService);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(centralizedExam()));
        when(schoolUserRepository.existsBySchoolIdAndUserId(SCHOOL_ID, TARGET_USER_ID)).thenReturn(true);
        when(examMemberRepository.save(any(ExamMember.class))).thenAnswer(inv -> inv.getArgument(0));
        givenCallerIsChair();
    }

    // --- Chủ tịch hội đồng ---

    @Test
    void should_let_the_chair_add_an_author() {
        var result = createUseCase.execute(new CreateExamMemberCommand(EXAM_ID, TARGET_USER_ID, "AUTHOR"));

        assertThat(result.role()).isEqualTo(ExamMemberRole.AUTHOR.name());
        verify(examMemberRepository).save(any(ExamMember.class));
    }

    @Test
    void should_let_the_chair_add_a_reviewer() {
        var result = createUseCase.execute(new CreateExamMemberCommand(EXAM_ID, TARGET_USER_ID, "REVIEWER"));

        assertThat(result.role()).isEqualTo(ExamMemberRole.REVIEWER.name());
    }

    @Test
    void should_reject_the_chair_appointing_another_chair() {
        assertThatThrownBy(() -> createUseCase.execute(new CreateExamMemberCommand(EXAM_ID, TARGET_USER_ID, "CHAIR")))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Chỉ quản trị trường mới bổ nhiệm");
        verify(examMemberRepository, never()).save(any());
    }

    @Test
    void should_let_the_chair_swap_an_author_to_reviewer() {
        givenExistingMember(ExamMemberRole.AUTHOR);

        var result = updateUseCase.execute(new UpdateExamMemberCommand(EXAM_ID, MEMBER_ID, "REVIEWER"));

        assertThat(result.role()).isEqualTo(ExamMemberRole.REVIEWER.name());
    }

    /** Vai đích không phải CHAIR nhưng vai đang bị gỡ thì có — bỏ sót vế này là chủ tịch hạ cấp nhau. */
    @Test
    void should_reject_the_chair_demoting_another_chair() {
        givenExistingMember(ExamMemberRole.CHAIR);

        assertThatThrownBy(() -> updateUseCase.execute(new UpdateExamMemberCommand(EXAM_ID, MEMBER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Chỉ quản trị trường mới bổ nhiệm");
        verify(examMemberRepository, never()).save(any());
    }

    @Test
    void should_let_the_chair_remove_an_author() {
        givenExistingMember(ExamMemberRole.AUTHOR);

        deleteUseCase.execute(new DeleteExamMemberCommand(EXAM_ID, MEMBER_ID));

        verify(examMemberRepository).deleteById(MEMBER_ID);
    }

    @Test
    void should_reject_the_chair_removing_a_chair() {
        givenExistingMember(ExamMemberRole.CHAIR);

        assertThatThrownBy(() -> deleteUseCase.execute(new DeleteExamMemberCommand(EXAM_ID, MEMBER_ID)))
            .isInstanceOf(ForbiddenException.class);
        verify(examMemberRepository, never()).deleteById(any());
    }

    // --- Quản trị trường ---

    @Test
    void should_let_the_school_admin_appoint_a_chair() {
        givenCallerIsSchoolAdmin();

        var result = createUseCase.execute(new CreateExamMemberCommand(EXAM_ID, TARGET_USER_ID, "CHAIR"));

        assertThat(result.role()).isEqualTo(ExamMemberRole.CHAIR.name());
    }

    @Test
    void should_let_the_school_admin_remove_a_chair() {
        givenCallerIsSchoolAdmin();
        givenExistingMember(ExamMemberRole.CHAIR);

        deleteUseCase.execute(new DeleteExamMemberCommand(EXAM_ID, MEMBER_ID));

        verify(examMemberRepository).deleteById(MEMBER_ID);
    }

    // --- Người ngoài ---

    @Test
    void should_reject_a_member_who_is_neither_school_admin_nor_chair() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, CALLER_ID, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> createUseCase.execute(new CreateExamMemberCommand(EXAM_ID, TARGET_USER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
    }

    /** Quản trị trường khác trường không được đụng vào hội đồng của trường này. */
    @Test
    void should_reject_a_school_admin_from_another_school() {
        givenCallerIsSchoolAdmin();
        var otherSchool = mock(SchoolUser.class);
        when(otherSchool.getSchoolId()).thenReturn(UUID.randomUUID());
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(otherSchool));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, CALLER_ID, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> createUseCase.execute(new CreateExamMemberCommand(EXAM_ID, TARGET_USER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class);
    }

    /** Bài trên lớp không có hội đồng: một CHAIR duy nhất do use case tạo bài tự gán. */
    @Test
    void should_reject_managing_members_of_a_class_test() {
        var classTest = centralizedExam();
        classTest.setKind(ExamKind.CLASS_TEST);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTest));

        assertThatThrownBy(() -> createUseCase.execute(new CreateExamMemberCommand(EXAM_ID, TARGET_USER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class);
    }

    private void givenCallerIsChair() {
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(EXAM_ID, CALLER_ID, ExamMemberRole.CHAIR))
            .thenReturn(true);
    }

    private void givenCallerIsSchoolAdmin() {
        var schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
    }

    private void givenExistingMember(ExamMemberRole role) {
        var member = new ExamMember(EXAM_ID, TARGET_USER_ID, role, Instant.now(), CALLER_ID);
        member.setId(MEMBER_ID);
        when(examMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
    }

    private Exam centralizedExam() {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setSchoolId(SCHOOL_ID);
        exam.setKind(ExamKind.CENTRALIZED);
        return exam;
    }
}
