package com.sep.vox.application.usecase.schoolgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class CreateSchoolGradeUseCaseTests {

    private SchoolGradeRepository schoolGradeRepository;
    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private CreateSchoolGradeUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID gradeLevelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSchoolGradeUseCase(
            schoolGradeRepository,
            schoolGradeLevelRepository,
            userRepository,
            schoolUserRepository,
            userContextPort
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
    }

    @Test
    void should_create_grade_with_active_status() {
        var now = OffsetDateTime.now();
        var gradeLevel = new SchoolGradeLevel(
            gradeLevelId, schoolId, "K1", "Khối 1", "desc", 1, SchoolGradeLevelStatus.ACTIVE, now, now,
            UUID.randomUUID(), UUID.randomUUID());
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(gradeLevel));
        when(schoolGradeRepository.existsBySchoolGradeLevelIdAndCode(any(), any())).thenReturn(false);
        when(schoolGradeRepository.save(any(SchoolGrade.class))).thenAnswer(invocation -> {
            SchoolGrade toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        var command = new CreateSchoolGradeCommand(
            schoolId, gradeLevelId, "nh2024", "Năm học 2024", "desc",
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30));
        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(SchoolGrade.class);
        org.mockito.Mockito.verify(schoolGradeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchoolGradeStatus.ACTIVE);
    }
}
