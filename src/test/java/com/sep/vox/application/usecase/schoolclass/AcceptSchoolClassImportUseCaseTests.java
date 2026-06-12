package com.sep.vox.application.usecase.schoolclass;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class AcceptSchoolClassImportUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private SchoolClassRepository schoolClassRepository;
    private SupportedLanguageRepository supportedLanguageRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private AcceptSchoolClassImportUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        useCase = new AcceptSchoolClassImportUseCase(
            importSessionRepository,
            importRowRepository,
            schoolClassRepository,
            supportedLanguageRepository,
            schoolGradeRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            jsonSerializationPort,
            TestSchoolUserRepository.create()
        );
    }

    @Test
    void execute_should_import_valid_rows_and_mark_invalid_rows() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var session = previewedSession(sessionId, schoolId);
        var rows = List.of(
            row(sessionId, 1L, Map.of("Mã lớp", "ENG-01", "Tên lớp", "English 01", "Ngôn ngữ", "EN", "Khối", "G10"), jsonSerializationPort),
            row(sessionId, 2L, Map.of("Mã lớp", "", "Tên lớp", "Missing code", "Ngôn ngữ", "EN", "Khối", "G10"), jsonSerializationPort)
        );
        var mapping = Map.of(
            "Mã lớp", "code",
            "Tên lớp", "name",
            "Ngôn ngữ", "languageCode",
            "Khối", "schoolGradeCode"
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId)).thenReturn(rows);
        when(supportedLanguageRepository.findByCodeIn(Set.of("EN"))).thenReturn(List.of(activeLanguage(languageId, "EN")));
        when(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10"))).thenReturn(List.of(activeGrade(gradeId, schoolId, "G10")));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of());
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(new AcceptSchoolClassImportCommand(schoolId, sessionId, mapping));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.invalidRows()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(schoolClassRepository).save(any(SchoolClass.class));
        verify(schoolClassRepository).findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"));
        verify(supportedLanguageRepository).findByCodeIn(Set.of("EN"));
        verify(schoolGradeRepository).findBySchoolIdAndCodeIn(schoolId, Set.of("G10"));
        verify(schoolClassRepository, never()).findBySchoolIdAndCode(any(), any());
        verify(supportedLanguageRepository, never()).findByCode(any());
        verify(schoolGradeRepository, never()).findBySchoolIdAndCode(any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportRow>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(importRowRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue().get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rowsCaptor.getValue().get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rowsCaptor.getValue().get(1).getErrorsJson()).contains("code");
    }

    @Test
    void execute_should_throw_when_required_mapping_is_missing() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var mapping = Map.of("Mã lớp", "code");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(previewedSession(sessionId, schoolId)));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new AcceptSchoolClassImportCommand(schoolId, sessionId, mapping)));

        verify(importRowRepository, never()).findBySessionIdOrderByRowNumber(any());
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void execute_should_throw_when_requested_school_differs_from_current_user_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var mapping = Map.of(
            "MÃ£ lá»›p", "code",
            "TÃªn lá»›p", "name",
            "NgÃ´n ngá»¯", "languageCode",
            "Khá»‘i", "schoolGradeCode"
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new AcceptSchoolClassImportCommand(UUID.randomUUID(), sessionId, mapping)));

        verify(importSessionRepository, never()).findById(any());
        verify(importRowRepository, never()).findBySessionIdOrderByRowNumber(any());
    }

    private static ImportSession previewedSession(UUID id, UUID schoolId) {
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
            2L,
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

    private static ImportRow row(UUID sessionId, long rowNumber, Map<String, String> rawData, FakeJsonSerializationPort jsonSerializationPort) {
        return new ImportRow(UUID.randomUUID(), sessionId, rowNumber, jsonSerializationPort.toJson(rawData), null, null, ImportRowStatus.PENDING);
    }

    private static User activeUser(UUID id, UUID schoolId) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static SupportedLanguage activeLanguage(UUID id, String code) {
        var language = new SupportedLanguage();
        language.setId(id);
        language.setCode(new LanguageCode(code));
        language.setActive(true);
        return language;
    }

    private static SchoolGrade activeGrade(UUID id, UUID schoolId, String code) {
        var grade = new SchoolGrade();
        grade.setId(id);
        grade.setCode(code);
        grade.setStatus(SchoolGradeStatus.ACTIVE);
        return grade;
    }
}
