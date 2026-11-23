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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class DeleteSchoolGradeUseCaseTests {

    private SchoolGradeRepository schoolGradeRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
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
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new DeleteSchoolGradeUseCase(
            schoolGradeRepository,
            schoolClassRepository,
            schoolClassUserRepository,
            userContextPort,
            userRepository,
            schoolUserRepository
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(SchoolUser.builder()
                .id(UUID.randomUUID())
                .schoolId(schoolId)
                .userId(currentUserId)
                .startDate(null)
                .endDate(null)
                .build()));
    }

    @Test
    void should_cascade_soft_delete_classes_and_memberships_then_archive_grade() {
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(SchoolGradeStatus.ACTIVE)));

        useCase.execute(new DeleteSchoolGradeCommand(schoolId, gradeId));

        // Deactivates memberships + archives the classes that belong to the school year.
        verify(schoolClassUserRepository).deactivateByGradeId(eq(gradeId), any(Instant.class));
        verify(schoolClassRepository).archiveByGradeId(eq(gradeId), any(Instant.class), eq(currentUserId.toString()));

        // Archives the school year itself (soft delete), no hard delete.
        ArgumentCaptor<SchoolGrade> captor = ArgumentCaptor.forClass(SchoolGrade.class);
        verify(schoolGradeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchoolGradeStatus.ARCHIVED);
        verify(schoolGradeRepository, never()).deleteById(any());
    }

    @Test
    void should_reject_when_grade_already_archived() {
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(SchoolGradeStatus.ARCHIVED)));

        assertThatThrownBy(() -> useCase.execute(new DeleteSchoolGradeCommand(schoolId, gradeId)))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(schoolClassUserRepository, never()).deactivateByGradeId(any(), any());
        verify(schoolClassRepository, never()).archiveByGradeId(any(), any(), any());
        verify(schoolGradeRepository, never()).save(any());
        verify(schoolGradeRepository, never()).deleteById(any());
    }

    private SchoolGrade grade(SchoolGradeStatus status) {
        Instant now = Instant.now();
        return SchoolGrade.builder()
            .id(gradeId)
            .schoolId(schoolId)
            .gradeLevelId(gradeLevelId)
            .code("NH2024")
            .name("Năm học 2024")
            .description("desc")
            .startDate(LocalDate.of(2024, 9, 1))
            .endDate(LocalDate.of(2025, 6, 30))
            .status(status)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(UUID.randomUUID().toString())
            .updatedBy(UUID.randomUUID().toString())
            .build();
    }

}
