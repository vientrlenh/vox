package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolUserDetailsQuery;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

public class ViewSchoolUserUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private ViewSchoolUserDetailsUseCase viewSchoolUserUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        viewSchoolUserUseCase = new ViewSchoolUserDetailsUseCase(
            userContextPort, userRepository, schoolUserRepository
        );
    }

    @Test
    void view_should_return_school_user_when_caller_authorized() {
        var schoolUserId = UUID.randomUUID();
        var schoolUser = schoolUser(schoolUserId, schoolId, targetId);
        var query = new ViewSchoolUserDetailsQuery(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.existsBySchoolIdAndUserId(callerId, schoolId)).thenReturn(true);
        when(schoolUserRepository.findBySchoolIdAndUserId(schoolId, targetId)).thenReturn(Optional.of(schoolUser));

        var result = viewSchoolUserUseCase.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(schoolUserId);
        assertThat(result.schoolId()).isEqualTo(schoolId);
        assertThat(result.userId()).isEqualTo(targetId);
    }

    @Test
    void view_should_return_school_user_for_system_admin() {
        var schoolUserId = UUID.randomUUID();
        var schoolUser = schoolUser(schoolUserId, schoolId, targetId);
        var query = new ViewSchoolUserDetailsQuery(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(schoolUserRepository.findBySchoolIdAndUserId(schoolId, targetId)).thenReturn(Optional.of(schoolUser));

        var result = viewSchoolUserUseCase.execute(query);

        assertThat(result.userId()).isEqualTo(targetId);
    }

    @Test
    void view_should_throw_when_caller_is_inactive() {
        var query = new ViewSchoolUserDetailsQuery(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> viewSchoolUserUseCase.execute(query));
    }

    @Test
    void view_should_throw_when_caller_not_in_school() {
        var query = new ViewSchoolUserDetailsQuery(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.existsBySchoolIdAndUserId(callerId, schoolId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> viewSchoolUserUseCase.execute(query));
    }

    @Test
    void view_should_throw_when_school_user_not_found() {
        var query = new ViewSchoolUserDetailsQuery(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.existsBySchoolIdAndUserId(callerId, schoolId)).thenReturn(true);
        when(schoolUserRepository.findBySchoolIdAndUserId(schoolId, targetId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> viewSchoolUserUseCase.execute(query));
    }

    private SchoolUser schoolUser(UUID id, UUID userSchoolId, UUID userId) {
        var now = OffsetDateTime.now();
        return new SchoolUser(id, userSchoolId, userId, now, now.plusYears(1));
    }
}
