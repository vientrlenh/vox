package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewMyClassMembersQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.MyClassAccessGuard;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassMembersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewMyClassMembersUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private ViewMyClassMembersUseCase useCase;

    private final UUID callerId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        useCase = new ViewMyClassMembersUseCase(
            new MyClassAccessGuard(
                userContextPort,
                userRepository,
                mock(SchoolUserRepository.class),
                schoolClassUserRepository
            ),
            schoolClassUserRepository
        );
    }

    @Test
    void should_return_members_of_the_class() {
        var member = member(UUID.randomUUID());
        givenActiveCaller();
        givenMembership();
        when(schoolClassUserRepository.findBySchoolClassId(classId, null, null, 1, 20))
            .thenReturn(new PageResult<>(List.of(member), 1, 20, 1, 1));

        var result = useCase.execute(new ViewMyClassMembersQuery(classId, null, null, 1, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().userId()).isEqualTo(member.getUserId());
    }

    @Test
    void should_normalize_role_code_and_search_before_querying() {
        givenActiveCaller();
        givenMembership();
        when(schoolClassUserRepository.findBySchoolClassId(eq(classId), any(), any(), eq(1), eq(20)))
            .thenReturn(new PageResult<>(List.of(), 1, 20, 0, 0));

        useCase.execute(new ViewMyClassMembersQuery(classId, " student ", "  an  binh ", 1, 20));

        verify(schoolClassUserRepository).findBySchoolClassId(classId, "STUDENT", "an binh", 1, 20);
    }

    @Test
    void should_not_expose_roster_to_a_teacher_outside_the_class() {
        givenActiveCaller();
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(callerId, classId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new ViewMyClassMembersQuery(classId, null, null, 1, 20))
        );

        assertThat(exception.getMessage()).isEqualTo("Không tìm thấy lớp học");
        verify(schoolClassUserRepository, never()).findBySchoolClassId(classId, null, null, 1, 20);
    }

    private void givenActiveCaller() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(true);
    }

    private void givenMembership() {
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(callerId, classId))
            .thenReturn(Optional.of(member(callerId)));
    }

    private SchoolClassUser member(UUID userId) {
        return new SchoolClassUser(
            UUID.randomUUID(),
            userId,
            classId,
            true,
            OffsetDateTime.now(),
            null,
            UUID.randomUUID()
        );
    }
}
