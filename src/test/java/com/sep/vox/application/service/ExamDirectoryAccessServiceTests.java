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
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService.ExamDirectoryScope;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Ranh giới phân quyền của "danh bạ kỳ thi".
 *
 * <p>Luật ở đây phải khớp từng dòng với {@code authorize(Exam)} của các use case nhập
 * thí sinh — quyền đọc lệch quyền ghi chính là lỗi mà lớp service này sinh ra để vá.
 */
class ExamDirectoryAccessServiceTests {

    private SchoolUserRepository schoolUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private ExamMemberRepository examMemberRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ExamDirectoryAccessService service;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        service = new ExamDirectoryAccessService(
            mock(ExamRepository.class), schoolUserRepository, schoolClassRepository,
            examMemberRepository, userRoleQueryRepository, userContextPort);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
    }

    @Test
    void should_list_only_active_classes_the_caller_belongs_to() {
        var classId = UUID.randomUUID();
        var schoolClass = SchoolClass.create(
            schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-01", "English 01", null,
            UUID.randomUUID(), Instant.now());
        schoolClass.setId(classId);
        when(schoolClassRepository.findByUserId(schoolId, callerId, null, SchoolClassStatus.ACTIVE, 1, 200))
            .thenReturn(new PageResult<>(List.of(schoolClass), 1, 200, 1, 1));

        var classIds = service.callerClassIds(new ExamDirectoryScope(callerId, schoolId, false));

        assertThat(classIds).containsExactly(classId);
    }

    @Test
    void should_give_school_admin_school_wide_scope() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("SCHOOL_ADMIN");

        var scope = service.resolve(exam(ExamKind.CENTRALIZED));

        assertThat(scope.schoolWide()).isTrue();
        assertThat(scope.callerId()).isEqualTo(callerId);
        assertThat(scope.schoolId()).isEqualTo(schoolId);
    }

    @Test
    void should_give_school_admin_school_wide_scope_for_class_test_too() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("SCHOOL_ADMIN");

        assertThat(service.resolve(exam(ExamKind.CLASS_TEST)).schoolWide()).isTrue();
    }

    @Test
    void should_reject_school_admin_of_another_school() {
        givenCallerInSchool(UUID.randomUUID());
        givenCallerRoles("SCHOOL_ADMIN");
        givenCallerIsChair(false);

        assertThrows(ForbiddenException.class, () -> service.resolve(exam(ExamKind.CENTRALIZED)));
    }

    @Test
    void should_give_chair_of_centralized_exam_school_wide_scope() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("TEACHER");
        givenCallerIsChair(true);

        var scope = service.resolve(exam(ExamKind.CENTRALIZED));

        assertThat(scope.schoolWide()).isTrue();
        assertThat(scope.schoolId()).isEqualTo(schoolId);
    }

    @Test
    void should_limit_chair_of_class_test_to_own_classes() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("TEACHER");
        givenCallerIsChair(true);

        var scope = service.resolve(exam(ExamKind.CLASS_TEST));

        assertThat(scope.schoolWide()).isFalse();
        assertThat(scope.callerId()).isEqualTo(callerId);
        assertThat(scope.schoolId()).isEqualTo(schoolId);
    }

    @Test
    void should_reject_teacher_who_is_not_chair() {
        givenCallerInSchool(schoolId);
        givenCallerRoles("TEACHER");
        givenCallerIsChair(false);

        assertThrows(ForbiddenException.class, () -> service.resolve(exam(ExamKind.CENTRALIZED)));
    }

    @Test
    void should_reject_chair_from_another_school() {
        // Chair nhưng school_user trỏ trường khác: scope phải bám schoolId của kỳ thi,
        // và người này không được coi là hợp lệ.
        givenCallerInSchool(UUID.randomUUID());
        givenCallerRoles("TEACHER");
        givenCallerIsChair(false);

        assertThrows(ForbiddenException.class, () -> service.resolve(exam(ExamKind.CLASS_TEST)));
    }

    private Exam exam(ExamKind kind) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(kind);
        return exam;
    }

    private void givenCallerInSchool(UUID actualSchoolId) {
        var schoolUser = new SchoolUser();
        schoolUser.setUserId(callerId);
        schoolUser.setSchoolId(actualSchoolId);
        when(schoolUserRepository.findByUserId(callerId)).thenReturn(Optional.of(schoolUser));
    }

    private void givenCallerRoles(String... roleCodes) {
        var roles = java.util.Arrays.stream(roleCodes)
            .map(code -> new UserRoleInfo(
                UUID.randomUUID(), callerId, UUID.randomUUID(), Instant.now(), code, code))
            .toList();
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(callerId)).thenReturn(List.copyOf(roles));
    }

    private void givenCallerIsChair(boolean isChair) {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, callerId, ExamMemberRole.CHAIR))
            .thenReturn(isChair);
    }
}
