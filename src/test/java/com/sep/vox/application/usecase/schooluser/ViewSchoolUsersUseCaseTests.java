package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersBySchoolUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

public class ViewSchoolUsersUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        viewSchoolUsersBySchoolUseCase = new ViewSchoolUsersBySchoolUseCase(
            userContextPort,
            userRepository,
            schoolUserRepository
        );
    }

    @Test
    void list_should_return_page_of_school_users() {
        var userId = UUID.randomUUID();
        var schoolUser = schoolUser(UUID.randomUUID(), schoolId, userId);
        var page = new PageResult<>(List.of(schoolUser), 1, 20, 1, 1);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(true);
        when(schoolUserRepository.findBySchoolId(schoolId, null, null, null, 1, 20)).thenReturn(page);

        var result = viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null));

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userId()).isEqualTo(userId);
    }

    @Test
    void list_should_filter_by_role_id() {
        var roleId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var schoolUser = schoolUser(UUID.randomUUID(), schoolId, userId);
        var page = new PageResult<>(List.of(schoolUser), 1, 20, 1, 1);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(true);
        when(schoolUserRepository.findBySchoolId(schoolId, null, roleId, null, 1, 20)).thenReturn(page);

        var result = viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, roleId, null));

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userId()).isEqualTo(userId);
    }

    @Test
    void list_should_throw_when_caller_belongs_to_different_school() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(false);

        assertThrows(
            ForbiddenException.class,
            () -> viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null))
        );
    }

    @Test
    void list_should_throw_when_caller_is_inactive() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(false);

        assertThrows(
            UnauthorizedException.class,
            () -> viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null))
        );
    }

    private SchoolUser schoolUser(UUID id, UUID userSchoolId, UUID userId) {
        var now = Instant.now();
        return new SchoolUser(id, userSchoolId, userId, now, now.plus(365, ChronoUnit.DAYS));
    }
}
