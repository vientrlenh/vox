package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersForRequesterUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolUsersForRequesterUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private ViewSchoolUsersForRequesterUseCase useCase;

    private final UUID callerId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        useCase = new ViewSchoolUsersForRequesterUseCase(
            userContextPort, userRepository, schoolUserRepository, userRoleQueryRepository);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(true);
    }

    @Test
    void should_force_teacher_role_and_exclude_self_for_teacher_caller() {
        var teacherRoleId = UUID.randomUUID();
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(callerId))
            .thenReturn(List.of(roleInfo(teacherRoleId, "TEACHER")));
        when(schoolUserRepository.searchBySchoolId(eq(schoolId), eq(callerId), isNull(), eq(teacherRoleId), isNull(), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.<SchoolUser>of(), 1, 20, 0, 0));

        useCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, UUID.randomUUID(), null));

        // roleId của teacher được ép, bỏ qua roleId trong input; excludeUserId = callerId
        verify(schoolUserRepository).searchBySchoolId(eq(schoolId), eq(callerId), isNull(), eq(teacherRoleId), isNull(), eq(1), eq(20));
    }

    @Test
    void should_reject_teacher_without_teacher_role() {
        // Fail-safe: TEACHER (theo security) nhưng thiếu role row TEACHER trong DB -> từ chối,
        // không được rơi vào nhánh roleId=null (lộ toàn bộ học sinh).
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(callerId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null)))
            .isInstanceOf(ForbiddenException.class);
        verify(schoolUserRepository, never()).searchBySchoolId(any(), any(), any(), any(), any(),
            org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void should_honor_input_role_for_admin_caller() {
        var requestedRoleId = UUID.randomUUID();
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(callerId))
            .thenReturn(List.of(roleInfo(UUID.randomUUID(), "SCHOOL_ADMIN")));
        when(schoolUserRepository.searchBySchoolId(eq(schoolId), isNull(), isNull(), eq(requestedRoleId), isNull(), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.<SchoolUser>of(), 1, 20, 0, 0));

        useCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, requestedRoleId, null));

        verify(schoolUserRepository).searchBySchoolId(eq(schoolId), isNull(), isNull(), eq(requestedRoleId), isNull(), eq(1), eq(20));
    }

    private UserRoleInfo roleInfo(UUID roleId, String roleCode) {
        return new UserRoleInfo(UUID.randomUUID(), callerId, roleId, OffsetDateTime.now(), roleCode, roleCode);
    }
}
