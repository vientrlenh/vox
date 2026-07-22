package com.sep.vox.application.usecase.schoolgrade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradesQuery;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolGradesUseCaseTests {

    private SchoolGradeRepository schoolGradeRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private ViewSchoolGradesUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSchoolGradesUseCase(
            schoolGradeRepository,
            schoolRepository,
            userRepository,
            schoolUserRepository,
            userContextPort
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
    }

    @Test
    void should_pass_archived_status_to_repository() {
        grantAccess();
        when(schoolGradeRepository.findAllBySchoolId(eq(schoolId), isNull(), eq("ARCHIVED"), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.<SchoolGrade>of(), 1, 20, 0, 0));

        useCase.execute(new ViewSchoolGradesQuery(schoolId, null, "  archived ", 1, 20));

        verify(schoolGradeRepository).findAllBySchoolId(eq(schoolId), isNull(), eq("ARCHIVED"), eq(1), eq(20));
    }

    @Test
    void should_pass_null_status_when_blank() {
        grantAccess();
        when(schoolGradeRepository.findAllBySchoolId(eq(schoolId), isNull(), isNull(), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.<SchoolGrade>of(), 1, 20, 0, 0));

        useCase.execute(new ViewSchoolGradesQuery(schoolId, null, "  ", 1, 20));

        verify(schoolGradeRepository).findAllBySchoolId(eq(schoolId), isNull(), isNull(), eq(1), eq(20));
    }

    @Test
    void should_reject_invalid_status() {
        grantAccess();

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradesQuery(schoolId, null, "UNKNOWN", 1, 20)))
            .isInstanceOf(IllegalArgumentException.class);

        verify(schoolGradeRepository, never())
            .findAllBySchoolId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void should_reject_inactive_user() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradesQuery(schoolId, null, null, 1, 20)))
            .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(schoolGradeRepository);
    }

    @Test
    void should_reject_user_from_other_school() {
        // Chặn IDOR: SCHOOL_ADMIN của trường khác không được xem/list năm học của trường này.
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradesQuery(schoolId, null, null, 1, 20)))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(schoolGradeRepository);
    }

    @Test
    void should_reject_when_user_has_no_school() {
        // Fail-open đã đóng: user active nhưng không thuộc trường nào không được truy cập.
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradesQuery(schoolId, null, null, 1, 20)))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(schoolGradeRepository);
    }

    @Test
    void should_allow_system_admin_without_school() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(schoolGradeRepository.findAllBySchoolId(eq(schoolId), isNull(), isNull(), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.<SchoolGrade>of(), 1, 20, 0, 0));

        useCase.execute(new ViewSchoolGradesQuery(schoolId, null, null, 1, 20));

        verify(schoolGradeRepository).findAllBySchoolId(eq(schoolId), isNull(), isNull(), eq(1), eq(20));
    }

    @Test
    void should_reject_when_school_not_found() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradesQuery(schoolId, null, null, 1, 20)))
            .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(schoolGradeRepository);
    }

    private void grantAccess() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
    }
}
