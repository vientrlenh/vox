package com.sep.vox.application.usecase.schoolgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class CreateSchoolGradeUseCaseTests {

    private SchoolGradeRepository schoolGradeRepository;
    private GradeLevelRepository gradeLevelRepository;
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
        gradeLevelRepository = mock(GradeLevelRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSchoolGradeUseCase(
            schoolGradeRepository,
            gradeLevelRepository,
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
        var now = Instant.now();
        var gradeLevel = new GradeLevel(
            gradeLevelId, "K1", "Khối 1", "desc", 1, GradeLevelStatus.ACTIVE, now, now,
            UUID.randomUUID(), UUID.randomUUID());
        when(gradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(gradeLevel));
        when(schoolGradeRepository.existsBySchoolIdAndGradeLevelIdAndCode(any(), any(), any())).thenReturn(false);
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
