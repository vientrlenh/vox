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
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassUserImportFromFileUseCase;
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

class PreviewSchoolClassUserImportFromFileUseCaseTests {

    private FileProcessingPort fileProcessingPort;
    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PreviewSchoolClassUserImportFromFileUseCase useCase;

    @BeforeEach
    void setUp() {
        fileProcessingPort = mock(FileProcessingPort.class);
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        useCase = new PreviewSchoolClassUserImportFromFileUseCase(
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
        var file = UploadedFile.upload("class-users.csv", "text/csv", 48,
            "email,classCode\nstudent@example.com,ENG-01\n".getBytes(StandardCharsets.UTF_8));
        var parsed = new ParseImportFileResult(
            List.of("email", "classCode"),
            Map.of("email", "email", "classCode", "classCode"),
            List.of(Map.of("email", "student@example.com", "classCode", "ENG-01")),
            List.of(Map.of("email", "student@example.com", "classCode", "ENG-01")),
            1L
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(fileProcessingPort.parse(file, ImportType.SCHOOL_CLASS_USER)).thenReturn(parsed);
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> {
            var session = invocation.getArgument(0, ImportSession.class);
            session.setId(sessionId);
            return session;
        });

        var response = useCase.execute(new PreviewSchoolClassUserImportFromFileCommand(schoolId, file));

        assertThat(response.importSessionId()).isEqualTo(sessionId);
        assertThat(response.fileName()).isEqualTo("class-users.csv");
        assertThat(response.originalHeaders()).containsExactly("email", "classCode");
        assertThat(response.totalRows()).isEqualTo(1L);

        var sessionCaptor = ArgumentCaptor.forClass(ImportSession.class);
        verify(importSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getSchoolId()).isEqualTo(schoolId);
        assertThat(sessionCaptor.getValue().getType()).isEqualTo(ImportType.SCHOOL_CLASS_USER);
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(ImportSessionStatus.PREVIEWED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportRow>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(importRowRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(1);
        assertThat(rowsCaptor.getValue().get(0).getSessionId()).isEqualTo(sessionId);
        assertThat(rowsCaptor.getValue().get(0).getStatus()).isEqualTo(ImportRowStatus.PENDING);
        assertThat(rowsCaptor.getValue().get(0).getRawDataJson()).contains("student@example.com");
    }

    @Test
    void execute_should_throw_when_file_is_null() {
        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new PreviewSchoolClassUserImportFromFileCommand(UUID.randomUUID(), null)));

        verifyNoInteractions(userContextPort, userRepository, schoolRepository, fileProcessingPort, importSessionRepository, importRowRepository);
    }

    @Test
    void execute_should_throw_when_current_user_is_inactive() {
        var userId = UUID.randomUUID();
        var file = UploadedFile.upload("class-users.csv", "text/csv", 1, new byte[] { 1 });

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UUID.randomUUID(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new PreviewSchoolClassUserImportFromFileCommand(UUID.randomUUID(), file)));

        verifyNoInteractions(schoolRepository, fileProcessingPort, importSessionRepository, importRowRepository);
    }

    @Test
    void execute_should_throw_when_current_user_has_no_school() {
        var userId = UUID.randomUUID();
        var file = UploadedFile.upload("class-users.csv", "text/csv", 1, new byte[] { 1 });

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, null)));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new PreviewSchoolClassUserImportFromFileCommand(UUID.randomUUID(), file)));

        verifyNoInteractions(schoolRepository, fileProcessingPort, importSessionRepository, importRowRepository);
    }

    @Test
    void execute_should_throw_when_requested_school_differs_from_current_user_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var file = UploadedFile.upload("class-users.csv", "text/csv", 1, new byte[] { 1 });

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new PreviewSchoolClassUserImportFromFileCommand(UUID.randomUUID(), file)));

        verifyNoInteractions(schoolRepository, fileProcessingPort, importSessionRepository, importRowRepository);
    }

    @Test
    void execute_should_throw_when_school_is_inactive() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var file = UploadedFile.upload("class-users.csv", "text/csv", 1, new byte[] { 1 });
        var school = activeSchool(schoolId);
        school.setActive(false);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new PreviewSchoolClassUserImportFromFileCommand(schoolId, file)));

        verifyNoInteractions(fileProcessingPort, importSessionRepository, importRowRepository);
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
