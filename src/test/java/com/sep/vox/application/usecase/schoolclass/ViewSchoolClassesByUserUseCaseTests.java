package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolClassesByUserQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesByUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolClassesByUserUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private ViewSchoolClassesByUserUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        useCase = new ViewSchoolClassesByUserUseCase(
            userContextPort,
            userRepository,
            schoolUserRepository,
            schoolClassRepository
        );
    }

    @Test
    void list_should_return_page_of_school_classes() {
        var schoolClass = schoolClass(schoolId);
        var page = new PageResult<>(List.of(schoolClass), 1, 20, 1, 1);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, targetUserId)).thenReturn(true);
        when(schoolClassRepository.findByUserId(schoolId, targetUserId, null, 1, 20)).thenReturn(page);

        var result = useCase.execute(new ViewSchoolClassesByUserQuery(schoolId, targetUserId, null, 1, 20));

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(schoolClass.getId());
    }

    @Test
    void list_should_throw_when_caller_is_inactive() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(false);

        assertThrows(
            UnauthorizedException.class,
            () -> useCase.execute(new ViewSchoolClassesByUserQuery(schoolId, targetUserId, null, 1, 20))
        );
    }

    @Test
    void list_should_throw_when_caller_does_not_belong_to_school() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(false);

        assertThrows(
            ForbiddenException.class,
            () -> useCase.execute(new ViewSchoolClassesByUserQuery(schoolId, targetUserId, null, 1, 20))
        );
    }

    @Test
    void list_should_throw_when_target_user_does_not_belong_to_school() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, targetUserId)).thenReturn(false);

        assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new ViewSchoolClassesByUserQuery(schoolId, targetUserId, null, 1, 20))
        );
    }

    private SchoolClass schoolClass(UUID schoolId) {
        var schoolClass = SchoolClass.create(
            schoolId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01",
            "Starter class",
            UUID.randomUUID(),
            OffsetDateTime.now()
        );
        schoolClass.setId(UUID.randomUUID());
        return schoolClass;
    }
}
