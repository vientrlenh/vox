package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ListSchoolUsersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.SchoolUserInfo;
import com.sep.vox.application.query.repository.SchoolUserQueryRepository;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class ListSchoolUsersUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserQueryRepository schoolUserQueryRepository;
    private SchoolUserRepository schoolUserRepository;
    private ListSchoolUsersUseCase listSchoolUsersUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserQueryRepository = mock(SchoolUserQueryRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        listSchoolUsersUseCase = new ListSchoolUsersUseCase(
            userContextPort,
            userRepository,
            schoolUserQueryRepository,
            schoolUserRepository
        );
    }

    @Test
    void list_should_return_page_of_school_users() {
        var caller = callerUser(callerId, schoolId);
        var userId = UUID.randomUUID();
        var info = new SchoolUserInfo(
            userId,
            "student@school.edu.vn",
            "0987654321",
            "Nguyen Van A",
            "STUDENT",
            "INACTIVE",
            schoolId,
            OffsetDateTime.now(),
            UUID.randomUUID(),
            null,
            null
        );
        var page = new PageResult<>(List.of(info), 0, 20, 1, 1);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(schoolUserQueryRepository.findBySchoolIdAndRoleCodes(
            schoolId,
            List.of("STUDENT", "TEACHER"),
            new PageRequest(1, 20)
        )).thenReturn(page);

        var result = listSchoolUsersUseCase.execute(new ListSchoolUsersCommand(schoolId, 1, 20));

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        SchoolUserResponse response = result.content().get(0);
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.roleCode()).isEqualTo("STUDENT");

        verify(schoolUserQueryRepository).findBySchoolIdAndRoleCodes(
            schoolId,
            List.of("STUDENT", "TEACHER"),
            new PageRequest(1, 20)
        );
    }

    @Test
    void list_should_throw_when_caller_belongs_to_different_school() {
        var caller = callerUser(callerId, UUID.randomUUID());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        assertThrows(
            IllegalArgumentException.class,
            () -> listSchoolUsersUseCase.execute(new ListSchoolUsersCommand(schoolId, 1, 20))
        );

        verifyNoInteractions(schoolUserQueryRepository);
    }

    @Test
    void list_should_throw_when_caller_is_inactive() {
        var caller = callerUser(callerId, schoolId, UserStatus.INACTIVE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        assertThrows(
            UnauthorizedException.class,
            () -> listSchoolUsersUseCase.execute(new ListSchoolUsersCommand(schoolId, 1, 20))
        );

        verifyNoInteractions(schoolUserQueryRepository);
    }

    private User callerUser(UUID id, UUID userSchoolId) {
        return callerUser(id, userSchoolId, UserStatus.ACTIVE);
    }

    private User callerUser(UUID id, UUID userSchoolId, UserStatus status) {
        var now = OffsetDateTime.now();
        when(schoolUserRepository.findByUserId(id)).thenReturn(Optional.of(
            new SchoolUser(userSchoolId, id, now, now.plusYears(100))
        ));
        return new User(id, new Email("admin@school.edu.vn"), "hash",
            new Phone("0900000000"), new FullName("Admin User"), null,
            new DateOfBirth(LocalDate.of(1980, 1, 1)), "Admin Street", null,
            status, now, now, id, id);
    }
}
