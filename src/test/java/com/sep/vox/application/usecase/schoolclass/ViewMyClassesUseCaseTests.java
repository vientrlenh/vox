package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewMyClassesQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.MyClassAccessGuard;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewMyClassesUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private ViewMyClassesUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        useCase = new ViewMyClassesUseCase(
            new MyClassAccessGuard(userContextPort, userRepository, schoolUserRepository, schoolClassUserRepository),
            schoolClassRepository
        );
    }

    @Test
    void should_return_classes_the_caller_belongs_to() {
        var schoolClass = schoolClass();
        givenActiveCallerInSchool();
        when(schoolClassRepository.findByUserId(schoolId, callerId, null, null, 1, 10))
            .thenReturn(new PageResult<>(List.of(schoolClass), 1, 10, 1, 1));

        var result = useCase.execute(new ViewMyClassesQuery(schoolId, null, null, 1, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(schoolClass.getId());
        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    void should_pass_search_and_status_through_to_repository() {
        givenActiveCallerInSchool();
        when(schoolClassRepository.findByUserId(
                eq(schoolId), eq(callerId), any(), any(), eq(2), eq(20)))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(new ViewMyClassesQuery(schoolId, "  eng 01 ", "ACTIVE", 2, 20));

        verify(schoolClassRepository)
            .findByUserId(schoolId, callerId, "eng 01", SchoolClassStatus.ACTIVE, 2, 20);
    }

    @Test
    void should_reject_inactive_caller() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(false);

        assertThrows(
            UnauthorizedException.class,
            () -> useCase.execute(new ViewMyClassesQuery(schoolId, null, null, 1, 10))
        );
    }

    @Test
    void should_reject_caller_outside_the_school() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(false);

        assertThrows(
            ForbiddenException.class,
            () -> useCase.execute(new ViewMyClassesQuery(schoolId, null, null, 1, 10))
        );
    }

    @Test
    void should_reject_unknown_status() {
        givenActiveCallerInSchool();

        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new ViewMyClassesQuery(schoolId, null, "KHONG_TON_TAI", 1, 10))
        );
        assertThat(exception.getMessage()).isEqualTo("Trạng thái lớp học không hợp lệ");
    }

    private void givenActiveCallerInSchool() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)).thenReturn(true);
    }

    private SchoolClass schoolClass() {
        var schoolClass = SchoolClass.create(
            schoolId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01",
            "Starter class",
            UUID.randomUUID(),
            Instant.now()
        );
        schoolClass.setId(UUID.randomUUID());
        return schoolClass;
    }
}
