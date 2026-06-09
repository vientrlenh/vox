package com.sep.vox.application.usecase.schoolclass;

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
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

class PreviewSchoolClassImportFromFileUseCaseTests {

    private FileProcessingPort fileProcessingPort;
    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PreviewSchoolClassImportFromFileUseCase useCase;

    @BeforeEach
    void setUp() {
        fileProcessingPort = mock(FileProcessingPort.class);
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        useCase = new PreviewSchoolClassImportFromFileUseCase(
            fileProcessingPort,
            importSessionRepository,
            importRowRepository,
            userContextPort,
            userRepository,
            schoolRepository,
            jsonSerializationPort
        );
    }

    @Test
    void execute_should_create_preview_session_and_pending_rows() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var file = UploadedFile.upload("classes.csv", "text/csv", 28, "code,name\nENG-01,English 01\n".getBytes(StandardCharsets.UTF_8));
        var parsed = new ParseImportFileResult(
            List.of("code", "name"),
            Map.of("code", "code", "name", "name"),
            List.of(Map.of("code", "ENG-01", "name", "English 01")),
            List.of(Map.of("code", "ENG-01", "name", "English 01")),
            1L
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(fileProcessingPort.parse(file, ImportType.SCHOOL_CLASS)).thenReturn(parsed);
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> {
            var session = invocation.getArgument(0, ImportSession.class);
            session.setId(sessionId);
            return session;
        });

        var response = useCase.execute(new PreviewSchoolClassImportFromFileCommand(schoolId, file));

        assertThat(response.importSessionId()).isEqualTo(sessionId);
        assertThat(response.fileName()).isEqualTo("classes.csv");
        assertThat(response.originalHeaders()).containsExactly("code", "name");
        assertThat(response.totalRows()).isEqualTo(1L);

        var sessionCaptor = ArgumentCaptor.forClass(ImportSession.class);
        verify(importSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getSchoolId()).isEqualTo(schoolId);
        assertThat(sessionCaptor.getValue().getType()).isEqualTo(ImportType.SCHOOL_CLASS);
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(ImportSessionStatus.PREVIEWED);
        assertThat(sessionCaptor.getValue().getTotalRows()).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportRow>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(importRowRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(1);
        assertThat(rowsCaptor.getValue().get(0).getSessionId()).isEqualTo(sessionId);
        assertThat(rowsCaptor.getValue().get(0).getRowNumber()).isEqualTo(1L);
        assertThat(rowsCaptor.getValue().get(0).getStatus()).isEqualTo(ImportRowStatus.PENDING);
        assertThat(rowsCaptor.getValue().get(0).getRawDataJson()).contains("ENG-01");
    }

    @Test
    void execute_should_throw_when_current_user_is_inactive() {
        var userId = UUID.randomUUID();
        var file = UploadedFile.upload("classes.csv", "text/csv", 1, new byte[] { 1 });

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UUID.randomUUID(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new PreviewSchoolClassImportFromFileCommand(UUID.randomUUID(), file)));

        verifyNoInteractions(schoolRepository, fileProcessingPort, importSessionRepository, importRowRepository);
    }

    @Test
    void execute_should_throw_when_requested_school_differs_from_current_user_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var file = UploadedFile.upload("classes.csv", "text/csv", 1, new byte[] { 1 });

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new PreviewSchoolClassImportFromFileCommand(UUID.randomUUID(), file)));

        verifyNoInteractions(schoolRepository, fileProcessingPort, importSessionRepository, importRowRepository);
    }

    private static User activeUser(UUID id, UUID schoolId) {
        return user(id, schoolId, UserStatus.ACTIVE);
    }

    private static User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        user.setSchoolId(schoolId);
        user.setStatus(status);
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }
}
