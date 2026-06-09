package com.sep.vox.application.usecase.importfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.mapper.importfile.ImportSessionResponseMapper;
import com.sep.vox.application.port.input.query.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewImportSessionsUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private ViewImportSessionsUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewImportSessionsUseCase(
            importSessionRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            new ImportSessionResponseMapper(new FakeJsonSerializationPort())
        );
    }

    @Test
    void execute_should_return_page_for_current_school_with_filters() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var page = new PageResult<ImportSession>(List.of(), 1, 20, 0L, 0);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findBySchoolId(
            schoolId,
            ImportType.SCHOOL_CLASS,
            ImportSessionStatus.COMPLETED,
            new PageRequest(1, 20)
        )).thenReturn(page);

        var response = useCase.execute(new ViewImportSessionsQuery(1, 20, "SCHOOL_CLASS", "COMPLETED"));

        assertThat(response.content()).isEmpty();
        verify(importSessionRepository).findBySchoolId(
            schoolId,
            ImportType.SCHOOL_CLASS,
            ImportSessionStatus.COMPLETED,
            new PageRequest(1, 20)
        );
    }

    @Test
    void execute_should_throw_when_status_invalid() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new ViewImportSessionsQuery(1, 20, null, "UNKNOWN")));
    }

    private static User activeUser(UUID id, UUID schoolId) {
        var user = new User();
        user.setId(id);
        user.setSchoolId(schoolId);
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
