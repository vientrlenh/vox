package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schoolclass.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class UpdateSchoolClassUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private UpdateSchoolClassUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new UpdateSchoolClassUseCase(
            schoolClassRepository,
            schoolRepository,
            userRepository,
            userContextPort,
            schoolUserRepository
        );
    }

    @Test
    void update_name_only_should_return_id_when_atomic_update_succeeds() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UpdateSchoolClassCommand command = new UpdateSchoolClassCommand(classId, "  English   02  ", true, null, false, null, false);
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.updateMutableFields(
            eq(classId),
            eq(schoolId),
            eq("English 02"),
            eq(true),
            eq(null),
            eq(false),
            eq(null),
            eq(false)
        )).thenReturn(1);

        UpdateSchoolClassResponse response = useCase.execute(command);

        assertThat(response.schoolClassId()).isEqualTo(classId);
        verify(schoolClassRepository).updateMutableFields(
            eq(classId),
            eq(schoolId),
            eq("English 02"),
            eq(true),
            eq(null),
            eq(false),
            eq(null),
            eq(false)
        );
    }

    @Test
    void update_status_only_should_return_id_when_atomic_update_succeeds() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.updateMutableFields(
            eq(classId),
            eq(schoolId),
            eq(null),
            eq(false),
            eq(null),
            eq(false),
            eq(SchoolClassStatus.INACTIVE),
            eq(true)
        )).thenReturn(1);

        UpdateSchoolClassResponse response = useCase.execute(new UpdateSchoolClassCommand(classId, null, false, null, false, SchoolClassStatus.INACTIVE, true));

        assertThat(response.schoolClassId()).isEqualTo(classId);
    }

    @Test
    void update_description_null_should_pass_provided_flag_and_null_value() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.updateMutableFields(
            eq(classId),
            eq(schoolId),
            eq(null),
            eq(false),
            eq(null),
            eq(true),
            eq(null),
            eq(false)
        )).thenReturn(1);

        UpdateSchoolClassResponse response = useCase.execute(new UpdateSchoolClassCommand(classId, null, false, null, true, null, false));

        assertThat(response.schoolClassId()).isEqualTo(classId);
    }

    @Test
    void update_should_throw_when_input_is_empty() {
        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), null, false, null, false, null, false))
        );

        verifyNoInteractions(userContextPort, userRepository, schoolRepository, schoolClassRepository);
    }

    @Test
    void update_should_throw_when_no_rows_are_updated() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.updateMutableFields(
            eq(classId),
            eq(schoolId),
            eq("English 02"),
            eq(true),
            eq(null),
            eq(false),
            eq(null),
            eq(false)
        )).thenReturn(0);

        assertThrows(
            ResourceNotFoundException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(classId, "English 02", true, null, false, null, false))
        );
    }

    @Test
    void update_should_throw_when_status_is_provided_without_a_value() {
        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), null, false, null, false, null, true))
        );

        verifyNoInteractions(userContextPort, userRepository, schoolRepository, schoolClassRepository);
    }

    @Test
    void update_should_throw_when_name_is_blank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), "   ", true, null, false, null, false))
        );

        verifyNoInteractions(userContextPort, userRepository, schoolRepository, schoolClassRepository);
    }

    @Test
    void update_should_throw_when_name_is_too_long() {
        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), "A".repeat(256), true, null, false, null, false))
        );

        verifyNoInteractions(userContextPort, userRepository, schoolRepository, schoolClassRepository);
    }

    @Test
    void update_should_throw_when_description_is_too_long() {
        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), null, false, "A".repeat(2049), true, null, false))
        );

        verifyNoInteractions(userContextPort, userRepository, schoolRepository, schoolClassRepository);
    }

    @Test
    void update_should_throw_when_current_user_is_inactive() {
        UUID userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _u2 = user(userId, UUID.randomUUID(), UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_u2));

        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), "English 02", true, null, false, null, false))
        );

        verifyNoInteractions(schoolRepository, schoolClassRepository);
    }

    @Test
    void update_should_throw_when_current_user_has_no_school() {
        UUID userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user = user(userId, null, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));

        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), "English 02", true, null, false, null, false))
        );

        verifyNoInteractions(schoolRepository, schoolClassRepository);
    }

    @Test
    void update_should_throw_when_school_is_inactive() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        School school = activeSchool(schoolId);
        school.setActive(false);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user1 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user1));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new UpdateSchoolClassCommand(UUID.randomUUID(), "English 02", true, null, false, null, false))
        );

        verifyNoInteractions(schoolClassRepository);
    }

    private void stubActiveContext(UUID userId, UUID schoolId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user2 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user2));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
    }

    private User user(UUID id, UUID schoolId, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null ? Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(id)
                .startDate(java.time.Instant.now())
                .endDate(java.time.Instant.now().plus(36500, ChronoUnit.DAYS))
                .build()) : Optional.empty()
        );
        return user;
    }

    private static School activeSchool(UUID id) {
        School school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }
}
