package com.sep.vox.application.usecase.importfile;

import com.sep.vox.application.usecase.TestUserSchoolResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.mapper.importfile.ImportSessionResponseMapper;
import com.sep.vox.application.port.input.query.ViewImportSessionQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewImportSessionUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private ViewImportSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        useCase = new ViewImportSessionUseCase(
            importSessionRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            new ImportSessionResponseMapper(jsonSerializationPort),
            TestUserSchoolResolver.create()
        );
    }

    @Test
    void execute_should_return_details_for_current_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var session = session(sessionId, schoolId);
        session.setOriginalHeadersJson(jsonSerializationPort.toJson(java.util.List.of("Mã lớp")));
        session.setSuggestedMappingJson(jsonSerializationPort.toJson(java.util.Map.of("Mã lớp", "code")));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        var response = useCase.execute(new ViewImportSessionQuery(sessionId));

        assertThat(response.id()).isEqualTo(sessionId);
        assertThat(response.originalHeaders()).containsExactly("Mã lớp");
        assertThat(response.suggestedMapping()).hasSize(1);
    }

    @Test
    void execute_should_throw_when_session_belongs_to_other_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new ViewImportSessionQuery(sessionId)));
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

    private static User activeUser(UUID id, UUID schoolId) {
        var user = new User();
        user.setId(id);
        TestUserSchoolResolver.remember(id, schoolId);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }
}
