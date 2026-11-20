package com.sep.vox.application.usecase.assessment;

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
import com.sep.vox.application.port.input.command.CreateAssessmentMemberCommand;
import com.sep.vox.application.port.input.command.DeleteAssessmentMemberCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentMemberCommand;
import com.sep.vox.application.port.input.service.AssessmentMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.assessment.CreateAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentMemberUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Chủ tịch hội đồng tự lập hội đồng ra đề của kỳ thi mình chủ trì — trước đây ba endpoint này chỉ
 * nhận quản trị trường, nên giáo viên chủ tịch không qua nổi bước đầu tiên của quy trình.
 *
 * <p>Ranh giới còn lại: hàng CHAIR vẫn chỉ quản trị trường đụng được. Chủ tịch thu hồi vai trò của
 * chính mình hay của chủ tịch khác là kỳ thi mất người quyết định mà luồng giáo viên không dựng lại
 * được.
 */
class AssessmentMemberManagementTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;

    private CreateAssessmentMemberUseCase createUseCase;
    private UpdateAssessmentMemberUseCase updateUseCase;
    private DeleteAssessmentMemberUseCase deleteUseCase;

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        AssessmentMemberManageAccessService accessService = new AssessmentMemberManageAccessService(
            assessmentMemberRepository, schoolUserRepository, userRoleQueryRepository, userContextPort);
        createUseCase = new CreateAssessmentMemberUseCase(
            assessmentRepository, assessmentMemberRepository, schoolUserRepository, accessService);
        updateUseCase = new UpdateAssessmentMemberUseCase(assessmentRepository, assessmentMemberRepository, accessService);
        deleteUseCase = new DeleteAssessmentMemberUseCase(assessmentRepository, assessmentMemberRepository, accessService);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(centralizedAssessment()));
        when(schoolUserRepository.existsBySchoolIdAndUserId(SCHOOL_ID, TARGET_USER_ID)).thenReturn(true);
        when(assessmentMemberRepository.save(any(AssessmentMember.class))).thenAnswer(inv -> inv.getArgument(0));
        givenCallerIsChair();
    }

    // --- Chủ tịch hội đồng ---

    @Test
    void should_let_the_chair_add_an_author() {
        AssessmentMemberDto result = createUseCase.execute(new CreateAssessmentMemberCommand(ASSESSMENT_ID, TARGET_USER_ID, "AUTHOR"));

        assertThat(result.role()).isEqualTo(AssessmentMemberRole.AUTHOR.name());
        verify(assessmentMemberRepository).save(any(AssessmentMember.class));
    }

    @Test
    void should_let_the_chair_add_a_reviewer() {
        AssessmentMemberDto result = createUseCase.execute(new CreateAssessmentMemberCommand(ASSESSMENT_ID, TARGET_USER_ID, "REVIEWER"));

        assertThat(result.role()).isEqualTo(AssessmentMemberRole.REVIEWER.name());
    }

    @Test
    void should_reject_the_chair_appointing_another_chair() {
        assertThatThrownBy(() -> createUseCase.execute(new CreateAssessmentMemberCommand(ASSESSMENT_ID, TARGET_USER_ID, "CHAIR")))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Chỉ quản trị trường mới bổ nhiệm");
        verify(assessmentMemberRepository, never()).save(any());
    }

    @Test
    void should_let_the_chair_swap_an_author_to_reviewer() {
        givenExistingMember(AssessmentMemberRole.AUTHOR);

        AssessmentMemberDto result = updateUseCase.execute(new UpdateAssessmentMemberCommand(ASSESSMENT_ID, MEMBER_ID, "REVIEWER"));

        assertThat(result.role()).isEqualTo(AssessmentMemberRole.REVIEWER.name());
    }

    /** Vai đích không phải CHAIR nhưng vai đang bị gỡ thì có — bỏ sót vế này là chủ tịch hạ cấp nhau. */
    @Test
    void should_reject_the_chair_demoting_another_chair() {
        givenExistingMember(AssessmentMemberRole.CHAIR);

        assertThatThrownBy(() -> updateUseCase.execute(new UpdateAssessmentMemberCommand(ASSESSMENT_ID, MEMBER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Chỉ quản trị trường mới bổ nhiệm");
        verify(assessmentMemberRepository, never()).save(any());
    }

    @Test
    void should_let_the_chair_remove_an_author() {
        givenExistingMember(AssessmentMemberRole.AUTHOR);

        deleteUseCase.execute(new DeleteAssessmentMemberCommand(ASSESSMENT_ID, MEMBER_ID));

        verify(assessmentMemberRepository).deleteById(MEMBER_ID);
    }

    @Test
    void should_reject_the_chair_removing_a_chair() {
        givenExistingMember(AssessmentMemberRole.CHAIR);

        assertThatThrownBy(() -> deleteUseCase.execute(new DeleteAssessmentMemberCommand(ASSESSMENT_ID, MEMBER_ID)))
            .isInstanceOf(ForbiddenException.class);
        verify(assessmentMemberRepository, never()).deleteById(any());
    }

    // --- Quản trị trường ---

    @Test
    void should_let_the_school_admin_appoint_a_chair() {
        givenCallerIsSchoolAdmin();

        AssessmentMemberDto result = createUseCase.execute(new CreateAssessmentMemberCommand(ASSESSMENT_ID, TARGET_USER_ID, "CHAIR"));

        assertThat(result.role()).isEqualTo(AssessmentMemberRole.CHAIR.name());
    }

    @Test
    void should_let_the_school_admin_remove_a_chair() {
        givenCallerIsSchoolAdmin();
        givenExistingMember(AssessmentMemberRole.CHAIR);

        deleteUseCase.execute(new DeleteAssessmentMemberCommand(ASSESSMENT_ID, MEMBER_ID));

        verify(assessmentMemberRepository).deleteById(MEMBER_ID);
    }

    // --- Người ngoài ---

    @Test
    void should_reject_a_member_who_is_neither_school_admin_nor_chair() {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(ASSESSMENT_ID, CALLER_ID, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> createUseCase.execute(new CreateAssessmentMemberCommand(ASSESSMENT_ID, TARGET_USER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
    }

    /** A school admin from another school must not touch the council of this school. */
    @Test
    void should_reject_a_school_admin_from_another_school() {
        givenCallerIsSchoolAdmin();
        SchoolUser otherSchool = mock(SchoolUser.class);
        when(otherSchool.getSchoolId()).thenReturn(UUID.randomUUID());
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(otherSchool));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(ASSESSMENT_ID, CALLER_ID, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> createUseCase.execute(new CreateAssessmentMemberCommand(ASSESSMENT_ID, TARGET_USER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class);
    }

    /**
     * A class test has no council: a single CHAIR, assigned automatically by the use case that
     * creates the test.
     */
    @Test
    void should_reject_managing_members_of_a_class_test() {
        Assessment classTest = centralizedAssessment();
        classTest.setKind(AssessmentKind.CLASS_TEST);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(classTest));

        assertThatThrownBy(() -> createUseCase.execute(new CreateAssessmentMemberCommand(ASSESSMENT_ID, TARGET_USER_ID, "AUTHOR")))
            .isInstanceOf(ForbiddenException.class);
    }

    private void givenCallerIsChair() {
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(ASSESSMENT_ID, CALLER_ID, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
    }

    private void givenCallerIsSchoolAdmin() {
        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(CALLER_ID)).thenReturn(Optional.of(schoolUser));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(CALLER_ID)).thenReturn(List.of(
            new UserRoleInfo(
                UUID.randomUUID(), CALLER_ID, UUID.randomUUID(), Instant.now(), "SCHOOL_ADMIN", "Quản trị trường")
        ));
    }

    private void givenExistingMember(AssessmentMemberRole role) {
        AssessmentMember member = new AssessmentMember(ASSESSMENT_ID, TARGET_USER_ID, role, Instant.now(), CALLER_ID);
        member.setId(MEMBER_ID);
        when(assessmentMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
    }

    private Assessment centralizedAssessment() {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setSchoolId(SCHOOL_ID);
        assessment.setKind(AssessmentKind.CENTRALIZED);
        return assessment;
    }
}
