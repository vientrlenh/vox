package com.sep.vox.application.usecase.importfile;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.importfile.ImportRowResponseMapper;
import com.sep.vox.application.port.input.query.ViewImportRowsQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportRowsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
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
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewImportRowsUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private SchoolUserRepository schoolUserRepository;
    private ViewImportRowsUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new ViewImportRowsUseCase(
            importSessionRepository,
            importRowRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            new ImportRowResponseMapper(jsonSerializationPort),
            schoolUserRepository
        );
    }

    @Test
    void execute_should_return_rows_for_current_school_with_status_filter() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var row = row(sessionId, ImportRowStatus.INVALID, jsonSerializationPort);
        var page = new PageResult<>(List.of(row), 1, 20, 1L, 1);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionId(sessionId, ImportRowStatus.INVALID, new PageRequest(1, 20)))
            .thenReturn(page);

        var response = useCase.execute(new ViewImportRowsQuery(sessionId, 1, 20, "INVALID"));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).rawData()).extracting("key").containsExactly("Mã lớp");
        assertThat(response.content().get(0).mappedData()).extracting("key").containsExactly("code");
        assertThat(response.content().get(0).errors()).extracting("field").containsExactly("code");
        verify(importRowRepository).findBySessionId(sessionId, ImportRowStatus.INVALID, new PageRequest(1, 20));
    }

    @Test
    void execute_should_pass_null_status_when_status_blank() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user1 = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user1));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionId(sessionId, null, new PageRequest(2, 10)))
            .thenReturn(new PageResult<>(List.of(), 2, 10, 0L, 0));

        useCase.execute(new ViewImportRowsQuery(sessionId, 2, 10, " "));

        verify(importRowRepository).findBySessionId(sessionId, null, new PageRequest(2, 10));
    }

    @Test
    void execute_should_throw_when_session_belongs_to_other_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user2 = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user2));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new ViewImportRowsQuery(sessionId, 1, 20, null)));
    }

    @Test
    void execute_should_throw_when_session_not_found() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user3 = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user3));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(new ViewImportRowsQuery(sessionId, 1, 20, null)));
    }

    @Test
    void execute_should_throw_when_page_or_size_invalid() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new ViewImportRowsQuery(UUID.randomUUID(), 0, 20, null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new ViewImportRowsQuery(UUID.randomUUID(), 1, 0, null)));
    }

    @Test
    void execute_should_throw_when_status_invalid() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new ViewImportRowsQuery(UUID.randomUUID(), 1, 20, "UNKNOWN")));
    }

    @Test
    void execute_should_throw_when_current_user_inactive() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user4 = inactiveUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user4));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new ViewImportRowsQuery(UUID.randomUUID(), 1, 20, null)));
    }

    @Test
    void execute_should_throw_when_current_user_has_no_school() {
        var userId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user5 = activeUser(userId, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user5));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new ViewImportRowsQuery(UUID.randomUUID(), 1, 20, null)));
    }

    @Test
    void execute_should_throw_when_school_inactive() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user6 = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user6));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(inactiveSchool(schoolId)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new ViewImportRowsQuery(UUID.randomUUID(), 1, 20, null)));
    }

    private static ImportRow row(UUID sessionId, ImportRowStatus status, FakeJsonSerializationPort jsonSerializationPort) {
        return new ImportRow(
            UUID.randomUUID(),
            sessionId,
            1L,
            jsonSerializationPort.toJson(Map.of("Mã lớp", "A01")),
            jsonSerializationPort.toJson(Map.of("code", "A01")),
            jsonSerializationPort.toJson(List.of(Map.of("field", "code", "message", "Mã lớp đã tồn tại trong hệ thống"))),
            status
        );
    }

    private static ImportSession session(UUID id, UUID schoolId) {
        return new ImportSession(
            id,
            schoolId,
            ImportType.SCHOOL_CLASS,
            "classes.csv",
            "[]",
            "{}",
            null,
            0L,
            0L,
            0L,
            0L,
            1L,
            null,
            ImportSessionStatus.PREVIEWED,
            null,
            OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private User activeUser(UUID id, UUID schoolId) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setStatus(UserStatus.ACTIVE);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null ? Optional.of(new SchoolUser(schoolId, id, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now().plusYears(100))) : Optional.empty()
        );
        return user;
    }

    private User inactiveUser(UUID id, UUID schoolId) {
        var user = activeUser(id, schoolId);
        user.setStatus(UserStatus.INACTIVE);
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static School inactiveSchool(UUID id) {
        var school = activeSchool(id);
        school.setActive(false);
        return school;
    }
}
