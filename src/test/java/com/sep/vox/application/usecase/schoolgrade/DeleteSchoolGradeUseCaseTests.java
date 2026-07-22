package com.sep.vox.application.usecase.schoolgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class DeleteSchoolGradeUseCaseTests {

    private SchoolGradeRepository schoolGradeRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private DeleteSchoolGradeUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID gradeLevelId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new DeleteSchoolGradeUseCase(
            schoolGradeRepository,
            schoolClassRepository,
            schoolClassUserRepository,
            schoolGradeLevelRepository,
            userContextPort,
            userRepository,
            schoolUserRepository
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(new SchoolUser(UUID.randomUUID(), schoolId, currentUserId, null, null)));
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(gradeLevel()));
    }

    @Test
    void should_cascade_soft_delete_classes_and_memberships_then_archive_grade() {
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(SchoolGradeStatus.ACTIVE)));

        useCase.execute(new DeleteSchoolGradeCommand(schoolId, gradeId));

        // Vô hiệu hóa membership + archive lớp thuộc năm học
        verify(schoolClassUserRepository).deactivateByGradeId(eq(gradeId), any(OffsetDateTime.class));
        verify(schoolClassRepository).archiveByGradeId(eq(gradeId), any(OffsetDateTime.class), eq(currentUserId));

        // Archive chính năm học (xóa mềm), không hard-delete
        var captor = ArgumentCaptor.forClass(SchoolGrade.class);
        verify(schoolGradeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchoolGradeStatus.ARCHIVED);
        verify(schoolGradeRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_grade_already_archived() {
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(SchoolGradeStatus.ARCHIVED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteSchoolGradeCommand(schoolId, gradeId)))
            .isInstanceOf(NotFoundException.class);

        verify(schoolClassUserRepository, never()).deactivateByGradeId(any(), any());
        verify(schoolClassRepository, never()).archiveByGradeId(any(), any(), any());
        verify(schoolGradeRepository, never()).save(any());
        verify(schoolGradeRepository, never()).deleteById(any());
    }

    private SchoolGrade grade(SchoolGradeStatus status) {
        var now = OffsetDateTime.now();
        return new SchoolGrade(
            gradeId, gradeLevelId, "NH2024", "Năm học 2024", "desc",
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30), status,
            now, now, UUID.randomUUID(), UUID.randomUUID()
        );
    }

    private SchoolGradeLevel gradeLevel() {
        var now = OffsetDateTime.now();
        return new SchoolGradeLevel(
            gradeLevelId, schoolId, "K1", "Khối 1", "desc", 1, SchoolGradeLevelStatus.ACTIVE, now, now,
            UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
