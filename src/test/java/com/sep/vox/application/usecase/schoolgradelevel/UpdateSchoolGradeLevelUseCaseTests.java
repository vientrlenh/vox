package com.sep.vox.application.usecase.schoolgradelevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.UpdateSchoolGradeLevelUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class UpdateSchoolGradeLevelUseCaseTests {

    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private UpdateSchoolGradeLevelUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID gradeLevelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        useCase = new UpdateSchoolGradeLevelUseCase(
            schoolGradeLevelRepository,
            schoolUserRepository,
            userContextPort,
            userRepository
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
    }

    @Test
    void should_update_provided_fields_and_return_id() {
        grantAccess(schoolId);
        when(schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(
                eq(gradeLevelId), eq("Khối 1 mới"), eq("mô tả"), eq(2), any(Instant.class), eq(currentUserId)))
            .thenReturn(1);

        var result = useCase.execute(new UpdateSchoolGradeLevelCommand(
            schoolId, gradeLevelId, "  Khối 1 mới ", " mô tả ", 2));

        assertThat(result).isEqualTo(gradeLevelId);
        verify(schoolGradeLevelRepository).updateSchoolGradeLevelAtomic(
            eq(gradeLevelId), eq("Khối 1 mới"), eq("mô tả"), eq(2), any(Instant.class), eq(currentUserId));
    }

    @Test
    void should_pass_null_for_omitted_fields() {
        grantAccess(schoolId);
        var nameCaptor = ArgumentCaptor.forClass(String.class);
        var orderCaptor = ArgumentCaptor.forClass(Integer.class);
        when(schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(
                eq(gradeLevelId), any(), any(), any(), any(Instant.class), eq(currentUserId)))
            .thenReturn(1);

        useCase.execute(new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, null, null, 5));

        verify(schoolGradeLevelRepository).updateSchoolGradeLevelAtomic(
            eq(gradeLevelId), nameCaptor.capture(), isNull(), orderCaptor.capture(),
            any(Instant.class), eq(currentUserId));
        assertThat(nameCaptor.getValue()).isNull();
        assertThat(orderCaptor.getValue()).isEqualTo(5);
    }

    @Test
    void should_reject_blank_name() {
        grantAccess(schoolId);

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, "   ", null, null)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(schoolGradeLevelRepository, never())
            .updateSchoolGradeLevelAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_reject_non_positive_order() {
        grantAccess(schoolId);

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, null, null, 0)))
            .isInstanceOf(IllegalArgumentException.class);
        verify(schoolGradeLevelRepository, never())
            .updateSchoolGradeLevelAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_translate_duplicate_order_to_duplicated_exception() {
        grantAccess(schoolId);
        when(schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(any(), any(), any(), any(), any(), any()))
            .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, null, null, 3)))
            .isInstanceOf(DuplicatedException.class);
    }

    @Test
    void should_throw_not_found_when_no_rows_updated() {
        grantAccess(schoolId);
        when(schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(any(), any(), any(), any(), any(), any()))
            .thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, "Tên", null, null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_not_found_when_grade_level_missing() {
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, "Tên", null, null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_reject_inactive_user() {
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(newGradeLevel(schoolId)));
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, "Tên", null, null)))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_reject_user_from_other_school() {
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(newGradeLevel(schoolId)));
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(schoolUserOf(UUID.randomUUID())));

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, "Tên", null, null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throw_not_found_when_grade_level_belongs_to_another_school() {
        var otherSchoolId = UUID.randomUUID();
        when(schoolGradeLevelRepository.findById(gradeLevelId)).thenReturn(Optional.of(newGradeLevel(otherSchoolId)));
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                new UpdateSchoolGradeLevelCommand(schoolId, gradeLevelId, "Tên", null, null)))
            .isInstanceOf(NotFoundException.class);
    }

    private void grantAccess(UUID ownerSchoolId) {
        when(schoolGradeLevelRepository.findById(gradeLevelId))
            .thenReturn(Optional.of(newGradeLevel(ownerSchoolId)));
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(currentUserId)).thenReturn(Optional.of(schoolUserOf(ownerSchoolId)));
    }

    private SchoolGradeLevel newGradeLevel(UUID ownerSchoolId) {
        var now = Instant.now();
        return new SchoolGradeLevel(
            gradeLevelId, ownerSchoolId, "K1", "Khối 1", "desc", 1,
            SchoolGradeLevelStatus.ACTIVE, now, now, UUID.randomUUID(), UUID.randomUUID()
        );
    }

    private SchoolUser schoolUserOf(UUID userSchoolId) {
        var schoolUser = new SchoolUser();
        schoolUser.setSchoolId(userSchoolId);
        schoolUser.setUserId(currentUserId);
        return schoolUser;
    }
}
