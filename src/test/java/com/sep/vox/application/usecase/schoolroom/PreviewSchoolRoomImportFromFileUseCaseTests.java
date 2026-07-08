package com.sep.vox.application.usecase.schoolroom;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewSchoolRoomImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolroom.PreviewSchoolRoomImportFromFileUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class PreviewSchoolRoomImportFromFileUseCaseTests {

    private FileProcessingPort fileProcessingPort;
    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private SchoolUserRepository schoolUserRepository;
    private PreviewSchoolRoomImportFromFileUseCase useCase;

    @BeforeEach
    void setUp() {
        fileProcessingPort = mock(FileProcessingPort.class);
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new PreviewSchoolRoomImportFromFileUseCase(
            fileProcessingPort,
            importSessionRepository,
            importRowRepository,
            userContextPort,
            userRepository,
            schoolRepository,
            jsonSerializationPort,
            schoolUserRepository
        );
    }

    @Test
    void execute_should_create_preview_session_with_room_type() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var file = UploadedFile.upload("rooms.csv", "text/csv", 20, "code,name\nP101,Phòng 101\n".getBytes(StandardCharsets.UTF_8));
        var parsed = new ParseImportFileResult(
            List.of("code", "name"),
            Map.of("code", "code", "name", "name"),
            List.of(Map.of("code", "P101", "name", "Phòng 101")),
            List.of(Map.of("code", "P101", "name", "Phòng 101")),
            1L
        );

        var currentUser = activeUser(userId, schoolId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(fileProcessingPort.parse(file, ImportType.SCHOOL_ROOM)).thenReturn(parsed);
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> {
            var session = invocation.getArgument(0, ImportSession.class);
            session.setId(sessionId);
            return session;
        });

        var response = useCase.execute(new PreviewSchoolRoomImportFromFileCommand(schoolId, file));

        assertThat(response.importSessionId()).isEqualTo(sessionId);
        assertThat(response.totalRows()).isEqualTo(1L);

        var sessionCaptor = ArgumentCaptor.forClass(ImportSession.class);
        verify(importSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getSchoolId()).isEqualTo(schoolId);
        assertThat(sessionCaptor.getValue().getType()).isEqualTo(ImportType.SCHOOL_ROOM);
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(ImportSessionStatus.PREVIEWED);
        verify(importRowRepository).saveAll(any());
    }

    @Test
    void execute_should_throw_when_current_user_is_inactive() {
        var userId = UUID.randomUUID();
        var file = UploadedFile.upload("rooms.csv", "text/csv", 1, new byte[] { 1 });

        var inactive = user(userId, UUID.randomUUID(), UserStatus.INACTIVE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactive));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new PreviewSchoolRoomImportFromFileCommand(UUID.randomUUID(), file)));
        verifyNoInteractions(fileProcessingPort, importSessionRepository, importRowRepository);
    }

    @Test
    void execute_should_throw_when_requested_school_differs() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var file = UploadedFile.upload("rooms.csv", "text/csv", 1, new byte[] { 1 });

        var currentUser = activeUser(userId, schoolId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new PreviewSchoolRoomImportFromFileCommand(UUID.randomUUID(), file)));
        verifyNoInteractions(fileProcessingPort, importSessionRepository, importRowRepository);
    }

    private User activeUser(UUID id, UUID schoolId) {
        return user(id, schoolId, UserStatus.ACTIVE);
    }

    private User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setStatus(status);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null
                ? Optional.of(new SchoolUser(schoolId, id, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now().plusYears(100)))
                : Optional.empty()
        );
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }
}
