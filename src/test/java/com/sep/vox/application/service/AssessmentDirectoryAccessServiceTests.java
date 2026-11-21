package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService.AssessmentDirectoryScope;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * The permission boundary of the "assessment directory".
 *
 * <p>The rule here must match, line for line, the {@code authorize(Assessment)} of the candidate import
 * use cases -- a read permission that diverges from the write permission is exactly the bug this service class was created to patch.
 */
class AssessmentDirectoryAccessServiceTests {

    private SchoolUserRepository schoolUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private AssessmentDirectoryAccessService service;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        service = new AssessmentDirectoryAccessService(
            mock(AssessmentRepository.class), schoolUserRepository, schoolClassRepository,
            assessmentMemberRepository, userRoleQueryRepository, userContextPort);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
    }

    @Test
    void should_list_only_active_classes_the_caller_belongs_to() {
        UUID classId = UUID.randomUUID();
        SchoolClass schoolClass = SchoolClass.create(
            schoolId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01",
            null);
        schoolClass.setId(classId);
        when(schoolClassRepository.findByUserId(schoolId, callerId, null, SchoolClassStatus.ACTIVE, 1, 200))
            .thenReturn(new PageResult<>(List.of(schoolClass), 1, 200, 1, 1));

        List<UUID> classIds = service.callerClassIds(new AssessmentDirectoryScope(callerId, schoolId, false));

        assertThat(classIds).containsExactly(classId);
    }

    @Test
    void should_give_school_admin_school_wide_scope() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("SCHOOL_ADMIN");

        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = service.resolve(assessment(AssessmentKind.CENTRALIZED));

        assertThat(scope.schoolWide()).isTrue();
        assertThat(scope.callerId()).isEqualTo(callerId);
        assertThat(scope.schoolId()).isEqualTo(schoolId);
    }

    @Test
    void should_give_school_admin_school_wide_scope_for_class_test_too() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("SCHOOL_ADMIN");

        assertThat(service.resolve(assessment(AssessmentKind.CLASS_TEST)).schoolWide()).isTrue();
    }

    @Test
    void should_reject_school_admin_of_another_school() {
        givenCallerInSchool(UUID.randomUUID());
        givenCallerRoles("SCHOOL_ADMIN");
        givenCallerIsChair(false);

        assertThrows(ForbiddenException.class, () -> service.resolve(assessment(AssessmentKind.CENTRALIZED)));
    }

    @Test
    void should_give_chair_of_centralized_assessment_school_wide_scope() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("TEACHER");
        givenCallerIsChair(true);

        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = service.resolve(assessment(AssessmentKind.CENTRALIZED));

        assertThat(scope.schoolWide()).isTrue();
        assertThat(scope.schoolId()).isEqualTo(schoolId);
    }

    @Test
    void should_limit_chair_of_class_test_to_own_classes() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("TEACHER");
        givenCallerIsChair(true);

        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = service.resolve(assessment(AssessmentKind.CLASS_TEST));

        assertThat(scope.schoolWide()).isFalse();
        assertThat(scope.callerId()).isEqualTo(callerId);
        assertThat(scope.schoolId()).isEqualTo(schoolId);
    }

    @Test
    void should_reject_teacher_who_is_not_chair() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("TEACHER");
        givenCallerIsChair(false);

        assertThrows(ForbiddenException.class, () -> service.resolve(assessment(AssessmentKind.CENTRALIZED)));
    }

    @Test
    void should_reject_chair_from_another_school() {
        // Chair nhưng school_user trỏ trường khác: scope phải bám schoolId của kỳ thi,
        // và người này không được coi là hợp lệ.
        givenCallerInSchool(UUID.randomUUID());
        givenCallerRoles("TEACHER");
        givenCallerIsChair(false);

        assertThrows(ForbiddenException.class, () -> service.resolve(assessment(AssessmentKind.CLASS_TEST)));
    }

    private Assessment assessment(AssessmentKind kind) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(kind);
        return assessment;
    }

    private void givenCallerInSchool(UUID actualSchoolId) {
        SchoolUser schoolUser = new SchoolUser();
        schoolUser.setUserId(callerId);
        schoolUser.setSchoolId(actualSchoolId);
        when(schoolUserRepository.findByUserId(callerId)).thenReturn(Optional.of(schoolUser));
    }

    private void givenCallerRoles(String... roleCodes) {
        List<UserRoleInfo> roles = java.util.Arrays.stream(roleCodes)
            .map(code -> new UserRoleInfo(
                UUID.randomUUID(), callerId, UUID.randomUUID(), Instant.now(), code, code))
            .toList();
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(callerId)).thenReturn(List.copyOf(roles));
    }

    private void givenCallerIsChair(boolean isChair) {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, callerId, AssessmentMemberRole.CHAIR))
            .thenReturn(isChair);
    }
}
