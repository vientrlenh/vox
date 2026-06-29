package com.sep.vox.application.usecase.schoolgradelevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradeLevelDetailsQuery;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.ViewSchoolGradeLevelDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolGradeLevelDetailsUseCaseTests {

    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private ViewSchoolGradeLevelDetailsUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID gradeLevelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSchoolGradeLevelDetailsUseCase(
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
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(newGradeLevel(schoolId)));

        var result = useCase.execute(new ViewSchoolGradeLevelDetailsQuery(schoolId, gradeLevelId));

        assertThat(result.id()).isEqualTo(gradeLevelId);
        assertThat(result.schoolId()).isEqualTo(schoolId);
        assertThat(result.code()).isEqualTo("K1");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void should_reject_inactive_user() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelDetailsQuery(schoolId, gradeLevelId)))
            .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(schoolGradeLevelRepository);
    }

    @Test
    void should_reject_user_from_other_school() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelDetailsQuery(schoolId, gradeLevelId)))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(schoolGradeLevelRepository);
    }

    @Test
    void should_reject_when_grade_level_not_found() {
        grantAccess();
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelDetailsQuery(schoolId, gradeLevelId)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_reject_when_grade_level_belongs_to_another_school() {
        grantAccess();
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(newGradeLevel(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelDetailsQuery(schoolId, gradeLevelId)))
            .isInstanceOf(NotFoundException.class);
    }

    private void grantAccess() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
    }

    private SchoolGradeLevel newGradeLevel(UUID ownerSchoolId) {
        var now = OffsetDateTime.now();
        return new SchoolGradeLevel(
            gradeLevelId, ownerSchoolId, "K1", "Khối 1", "desc", 1,
            SchoolGradeLevelStatus.ACTIVE, now, now, UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
