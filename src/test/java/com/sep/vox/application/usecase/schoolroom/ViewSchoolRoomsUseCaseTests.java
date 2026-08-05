package com.sep.vox.application.usecase.schoolroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSchoolRoomsQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.MyClassAccessGuard;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * {@code schoolId} là tham số client gửi lên, không suy ra từ token — từ khi query này mở cho cả
 * giáo viên (để chọn phòng cho bài kiểm tra trên lớp) thì phải chặn đọc phòng của trường khác.
 */
class ViewSchoolRoomsUseCaseTests {

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID OTHER_SCHOOL_ID = UUID.randomUUID();

    private SchoolRoomRepository schoolRoomRepository;
    private SchoolUserRepository schoolUserRepository;
    private ViewSchoolRoomsUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolRoomRepository = mock(SchoolRoomRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        var userRepository = mock(UserRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new ViewSchoolRoomsUseCase(
            schoolRoomRepository,
            new MyClassAccessGuard(
                userContextPort,
                userRepository,
                schoolUserRepository,
                mock(SchoolClassUserRepository.class))
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(CALLER_ID);
        when(userRepository.existsByIdAndStatus(CALLER_ID, UserStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    void should_return_rooms_of_the_school_the_caller_belongs_to() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(SCHOOL_ID, CALLER_ID)).thenReturn(true);
        when(schoolRoomRepository.findAllBySchoolId(SCHOOL_ID, 1, 10))
            .thenReturn(new PageResult<>(List.of(room()), 1, 10, 1, 1));

        var result = useCase.execute(new ViewSchoolRoomsQuery(SCHOOL_ID, 1, 10));

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void should_reject_when_caller_does_not_belong_to_requested_school() {
        when(schoolUserRepository.existsBySchoolIdAndUserId(OTHER_SCHOOL_ID, CALLER_ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolRoomsQuery(OTHER_SCHOOL_ID, 1, 10)))
            .isInstanceOf(ForbiddenException.class);

        verify(schoolRoomRepository, never()).findAllBySchoolId(OTHER_SCHOOL_ID, 1, 10);
    }

    private SchoolRoom room() {
        var room = new SchoolRoom();
        room.setId(UUID.randomUUID());
        room.setSchoolId(SCHOOL_ID);
        room.setCode("P101");
        room.setName("Phòng 101");
        room.setActive(true);
        return room;
    }
}
