package com.sep.vox.application.usecase.schoolgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradeDetailsQuery;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradeDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolGradeDetailsUseCaseTests {

    private SchoolGradeRepository schoolGradeRepository;
    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private ViewSchoolGradeDetailsUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID gradeLevelId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSchoolGradeDetailsUseCase(
            schoolGradeRepository,
            schoolGradeLevelRepository,
            schoolRepository,
            userRepository,
            schoolUserRepository,
            userContextPort
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
    }

    @Test
    void should_return_detail_when_request_is_valid() {
        grantAccess();
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade()));
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(gradeLevel(schoolId)));

        var result = useCase.execute(new ViewSchoolGradeDetailsQuery(schoolId, gradeId));

        assertThat(result.id()).isEqualTo(gradeId);
        assertThat(result.code()).isEqualTo("NH2024");
    }

    @Test
    void should_reject_inactive_user() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeDetailsQuery(schoolId, gradeId)))
            .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(schoolGradeRepository);
    }

    @Test
    void should_reject_user_from_other_school() {
        // Chặn IDOR: SCHOOL_ADMIN của trường khác không được xem chi tiết năm học của trường này.
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeDetailsQuery(schoolId, gradeId)))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(schoolGradeRepository);
    }

    @Test
    void should_reject_when_user_has_no_school() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeDetailsQuery(schoolId, gradeId)))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(schoolGradeRepository);
    }

    @Test
    void should_reject_when_grade_not_found() {
        grantAccess();
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeDetailsQuery(schoolId, gradeId)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_reject_when_grade_belongs_to_another_school() {
        // Grade level (cầu nối) thuộc trường khác → không được lộ dữ liệu.
        grantAccess();
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade()));
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(gradeLevel(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeDetailsQuery(schoolId, gradeId)))
            .isInstanceOf(NotFoundException.class);
    }

    private void grantAccess() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
    }

    private SchoolGrade grade() {
        var now = Instant.now();
        return new SchoolGrade(
            gradeId, gradeLevelId, "NH2024", "Năm học 2024", "desc",
            LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30), SchoolGradeStatus.ACTIVE,
            now, now, UUID.randomUUID(), UUID.randomUUID()
        );
    }

    private SchoolGradeLevel gradeLevel(UUID ownerSchoolId) {
        var now = Instant.now();
        return new SchoolGradeLevel(
            gradeLevelId, ownerSchoolId, "K1", "Khối 1", "desc", 1, SchoolGradeLevelStatus.ACTIVE, now, now,
            UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
