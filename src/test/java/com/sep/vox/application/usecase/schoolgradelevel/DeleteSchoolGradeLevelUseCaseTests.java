package com.sep.vox.application.usecase.schoolgradelevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.DeleteSchoolGradeLevelUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class DeleteSchoolGradeLevelUseCaseTests {

    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private DeleteSchoolGradeLevelUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID gradeLevelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteSchoolGradeLevelUseCase(
            schoolGradeLevelRepository,
            schoolGradeRepository,
            userRepository,
            schoolUserRepository,
            userContextPort
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(new SchoolUser(UUID.randomUUID(), schoolId, currentUserId, null, null)));
    }

    @Test
    void should_soft_delete_active_grade_level_and_never_hard_delete() {
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(gradeLevel(SchoolGradeLevelStatus.ACTIVE)));
        when(schoolGradeRepository.existsBySchoolGradeLevelIdAndStatusNot(gradeLevelId, SchoolGradeStatus.ARCHIVED.name()))
            .thenReturn(false);

        useCase.execute(new DeleteSchoolGradeLevelCommand(schoolId, gradeLevelId));

        var captor = ArgumentCaptor.forClass(SchoolGradeLevel.class);
        verify(schoolGradeLevelRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchoolGradeLevelStatus.INACTIVE);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(currentUserId);
        verify(schoolGradeLevelRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_user_has_no_school() {
        when(schoolUserRepository.findByUserId(currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new DeleteSchoolGradeLevelCommand(schoolId, gradeLevelId)))
            .isInstanceOf(ForbiddenException.class);

        verify(schoolGradeLevelRepository, never()).save(any());
        verify(schoolGradeLevelRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_already_inactive_without_deleting() {
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(gradeLevel(SchoolGradeLevelStatus.INACTIVE)));

        assertThatThrownBy(() -> useCase.execute(new DeleteSchoolGradeLevelCommand(schoolId, gradeLevelId)))
            .isInstanceOf(NotFoundException.class);

        verify(schoolGradeLevelRepository, never()).save(any());
        verify(schoolGradeLevelRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_still_has_non_archived_grades() {
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(gradeLevel(SchoolGradeLevelStatus.ACTIVE)));
        when(schoolGradeRepository.existsBySchoolGradeLevelIdAndStatusNot(eq(gradeLevelId), eq(SchoolGradeStatus.ARCHIVED.name())))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new DeleteSchoolGradeLevelCommand(schoolId, gradeLevelId)))
            .isInstanceOf(IllegalStateException.class);

        verify(schoolGradeLevelRepository, never()).save(any());
        verify(schoolGradeLevelRepository, never()).deleteById(any());
    }

    private SchoolGradeLevel gradeLevel(SchoolGradeLevelStatus status) {
        var now = OffsetDateTime.now();
        return new SchoolGradeLevel(
            gradeLevelId, schoolId, "K1", "Khối 1", "desc", 1, status, now, now,
            UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
