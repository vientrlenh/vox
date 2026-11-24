package com.sep.vox.application.usecase.schoolroom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.command.schoolroom.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateSchoolRoomUseCaseTests {

    private final UUID roomId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock
    private SchoolRoomRepository schoolRoomRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserContextPort userContextPort;

    @InjectMocks
    private UpdateSchoolRoomUseCase useCase;

    @BeforeEach
    void setUp() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolRoomRepository.findById(roomId)).thenReturn(Optional.of(SchoolRoom.builder().id(roomId).build()));
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(schoolRoomRepository.updateSchoolRoomAtomic(any(), any(), any(), anyBoolean(),
            any(Instant.class), any())).thenReturn(1);
    }

    @Test
    void execute_clearsTheDescriptionWhenProvidedAsNull() {
        useCase.execute(new UpdateSchoolRoomCommand(roomId, null, null, true));

        verify(schoolRoomRepository).updateSchoolRoomAtomic(eq(roomId), isNull(), isNull(), eq(true),
            any(Instant.class), eq(userId.toString()));
    }

    @Test
    void execute_keepsTheDescriptionWhenNotProvided() {
        useCase.execute(new UpdateSchoolRoomCommand(roomId, "Room", null, false));

        verify(schoolRoomRepository).updateSchoolRoomAtomic(eq(roomId), eq("Room"), isNull(), eq(false),
            any(Instant.class), eq(userId.toString()));
    }
}
