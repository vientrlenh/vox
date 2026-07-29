package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewMyClassDetailsQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.MyClassAccessGuard;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewMyClassDetailsUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private ViewMyClassDetailsUseCase useCase;

    private final UUID callerId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        useCase = new ViewMyClassDetailsUseCase(
            new MyClassAccessGuard(
                userContextPort,
                userRepository,
                mock(SchoolUserRepository.class),
                schoolClassUserRepository
            ),
            schoolClassRepository
        );
    }

    @Test
    void should_return_class_when_caller_is_an_active_member() {
        var schoolClass = schoolClass();
        givenActiveCaller();
        givenMembership(true);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));

        var result = useCase.execute(new ViewMyClassDetailsQuery(classId));

        assertThat(result.id()).isEqualTo(schoolClass.getId());
        assertThat(result.code()).isEqualTo("ENG-01");
    }

    @Test
    void should_reject_inactive_caller() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> useCase.execute(new ViewMyClassDetailsQuery(classId)));
    }

    /**
     * Người ngoài lớp phải nhận đúng thông báo như khi lớp không tồn tại — nếu
     * khác nhau thì có thể dò được lớp nào đang tồn tại trong hệ thống.
     */
    @Test
    void should_hide_existence_of_class_the_caller_is_not_in() {
        givenActiveCaller();
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(callerId, classId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new ViewMyClassDetailsQuery(classId))
        );

        assertThat(exception.getMessage()).isEqualTo("Không tìm thấy lớp học");
        verify(schoolClassRepository, never()).findById(classId);
    }

    @Test
    void should_reject_member_who_already_left_the_class() {
        givenActiveCaller();
        givenMembership(false);

        var exception = assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new ViewMyClassDetailsQuery(classId))
        );

        assertThat(exception.getMessage()).isEqualTo("Không tìm thấy lớp học");
    }

    @Test
    void should_throw_not_found_when_class_row_is_missing() {
        givenActiveCaller();
        givenMembership(true);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(new ViewMyClassDetailsQuery(classId)));
    }

    private void givenActiveCaller() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
    }

    private void givenMembership(boolean isActive) {
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(callerId, classId))
            .thenReturn(Optional.of(new SchoolClassUser(
                UUID.randomUUID(),
                callerId,
                classId,
                isActive,
                OffsetDateTime.now(),
                isActive ? null : OffsetDateTime.now(),
                UUID.randomUUID()
            )));
    }

    private SchoolClass schoolClass() {
        var schoolClass = SchoolClass.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01",
            "Starter class",
            UUID.randomUUID(),
            OffsetDateTime.now()
        );
        schoolClass.setId(classId);
        return schoolClass;
    }
}
