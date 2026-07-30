package com.sep.vox.application.usecase.schoolgradelevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradeLevelsQuery;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.ViewSchoolGradeLevelsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolGradeLevelsUseCaseTests {

    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private ViewSchoolGradeLevelsUseCase useCase;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSchoolGradeLevelsUseCase(
            schoolGradeLevelRepository,
            schoolRepository,
            userRepository,
            schoolUserRepository,
            userContextPort
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
    }

    @Test
    void should_return_mapped_page_when_request_is_valid() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        var gradeLevel = newGradeLevel("K1", "Khối 1", 1, SchoolGradeLevelStatus.ACTIVE);
        when(schoolGradeLevelRepository.findBySchoolId(eq(schoolId), eq("k1"), eq(SchoolGradeLevelStatus.ACTIVE), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.of(gradeLevel), 1, 20, 1, 1));

        var result = useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, "  k1 ", "active"));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().code()).isEqualTo("K1");
        assertThat(result.content().getFirst().status()).isEqualTo("ACTIVE");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void should_default_to_active_status_when_status_is_blank() {
        // Mặc định ẩn Khối đã xóa mềm (INACTIVE): khi không lọc trạng thái thì chỉ trả về ACTIVE.
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(schoolGradeLevelRepository.findBySchoolId(eq(schoolId), eq(""), eq(SchoolGradeLevelStatus.ACTIVE), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.of(), 1, 20, 0, 0));

        useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, "  ", null));

        verify(schoolGradeLevelRepository).findBySchoolId(eq(schoolId), eq(""), eq(SchoolGradeLevelStatus.ACTIVE), eq(1), eq(20));
    }

    @Test
    void should_return_inactive_when_status_explicitly_inactive() {
        // Admin vẫn xem được Khối đã xóa mềm bằng cách truyền status=INACTIVE.
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(schoolGradeLevelRepository.findBySchoolId(eq(schoolId), isNull(), eq(SchoolGradeLevelStatus.INACTIVE), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.of(), 1, 20, 0, 0));

        useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, null, "inactive"));

        verify(schoolGradeLevelRepository).findBySchoolId(eq(schoolId), isNull(), eq(SchoolGradeLevelStatus.INACTIVE), eq(1), eq(20));
    }

    @Test
    void should_reject_inactive_user() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, null, null)))
            .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(schoolGradeLevelRepository);
    }

    @Test
    void should_reject_user_from_other_school() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, null, null)))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(schoolGradeLevelRepository);
    }

    @Test
    void should_reject_when_user_has_no_school() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, null, null)))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(schoolGradeLevelRepository);
    }

    @Test
    void should_reject_when_school_not_found() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, null, null)))
            .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(schoolGradeLevelRepository);
    }

    @Test
    void should_reject_invalid_status() {
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findSchoolIdByUserId(currentUserId)).thenReturn(Optional.of(schoolId));
        when(schoolRepository.existsById(schoolId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolGradeLevelsQuery(schoolId, 1, 20, null, "UNKNOWN")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(schoolGradeLevelRepository, org.mockito.Mockito.never())
            .findBySchoolId(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    private SchoolGradeLevel newGradeLevel(String code, String name, int order, SchoolGradeLevelStatus status) {
        var now = Instant.now();
        return new SchoolGradeLevel(
            UUID.randomUUID(), schoolId, code, name, "desc", order, status, now, now,
            UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
