package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.key.SchoolClassGradeKey;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.response.input.schoolclass.SchoolClassResponse;
import com.sep.vox.application.response.input.schoolclassuser.SchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;

import graphql.schema.DataFetchingEnvironment;

class SchoolControllerTests {

    @Test
    void school_classes_should_return_page_result() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, classUsersUseCase, updateUseCase);
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var expected = new PageResult<SchoolClassResponse>(List.of(), 1, 20, 0, 0);
        var query = new ViewSchoolClassesQuery(1, 20, "eng", "ACTIVE", languageId, gradeId);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.schoolClasses(1, 20, "eng", "ACTIVE", languageId, gradeId);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void school_classes_should_throw_when_page_or_size_invalid() {
        var controller = new SchoolController(
            mock(ViewSchoolsUseCase.class),
            mock(ViewSchoolClassesUseCase.class),
            mock(ViewSchoolClassDetailsUseCase.class),
            mock(ViewSchoolClassUsersUseCase.class),
            mock(UpdateSchoolClassUseCase.class)
        );

        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(0, 20, null, null, null, null));
        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(1, 0, null, null, null, null));
    }

    @Test
    void school_class_should_return_details() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, classUsersUseCase, updateUseCase);
        var classId = UUID.randomUUID();
        var expected = new SchoolClassResponse(
            classId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01",
            "Starter class",
            "ACTIVE",
            "2026-06-06T12:00:00Z",
            "2026-06-06T12:00:00Z"
        );
        when(detailsUseCase.execute(new ViewSchoolClassDetailsQuery(classId))).thenReturn(expected);

        var result = controller.schoolClass(classId);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewSchoolClassDetailsQuery(classId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_class_school_field_should_load_related_school() {
        var controller = new SchoolController(
            mock(ViewSchoolsUseCase.class),
            mock(ViewSchoolClassesUseCase.class),
            mock(ViewSchoolClassDetailsUseCase.class),
            mock(ViewSchoolClassUsersUseCase.class),
            mock(UpdateSchoolClassUseCase.class)
        );
        var schoolId = UUID.randomUUID();
        var response = schoolClassResponse(schoolId, UUID.randomUUID(), UUID.randomUUID());
        var expected = new SchoolDto(schoolId, "SCH", "School", null, null, null, null, null, 0, true, null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, SchoolDto>getDataLoader("schoolById")).thenReturn(loader);
        when(loader.load(schoolId)).thenReturn(CompletableFuture.completedFuture(expected));

        var result = controller.school(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(schoolId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_class_school_grade_field_should_load_related_grade_with_school_scope() {
        var controller = new SchoolController(
            mock(ViewSchoolsUseCase.class),
            mock(ViewSchoolClassesUseCase.class),
            mock(ViewSchoolClassDetailsUseCase.class),
            mock(ViewSchoolClassUsersUseCase.class),
            mock(UpdateSchoolClassUseCase.class)
        );
        var schoolId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var response = schoolClassResponse(schoolId, UUID.randomUUID(), gradeId);
        var expected = new SchoolGradeDto(gradeId, schoolId, "G10", "Grade 10", null, null, null, "ACTIVE", null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        var key = new SchoolClassGradeKey(gradeId, schoolId);
        when(env.<SchoolClassGradeKey, SchoolGradeDto>getDataLoader("schoolGradeByClass")).thenReturn(loader);
        when(loader.load(key)).thenReturn(CompletableFuture.completedFuture(expected));

        var result = controller.schoolGrade(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(key);
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_class_language_field_should_load_related_language() {
        var controller = new SchoolController(
            mock(ViewSchoolsUseCase.class),
            mock(ViewSchoolClassesUseCase.class),
            mock(ViewSchoolClassDetailsUseCase.class),
            mock(ViewSchoolClassUsersUseCase.class),
            mock(UpdateSchoolClassUseCase.class)
        );
        var languageId = UUID.randomUUID();
        var response = schoolClassResponse(UUID.randomUUID(), languageId, UUID.randomUUID());
        var expected = new SupportedLanguageDto(languageId, "EN", "English", null, true, null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, SupportedLanguageDto>getDataLoader("supportedLanguageById")).thenReturn(loader);
        when(loader.load(languageId)).thenReturn(CompletableFuture.completedFuture(expected));

        var result = controller.language(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(languageId);
    }

    @Test
    void school_class_users_should_return_page_result() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, classUsersUseCase, updateUseCase);
        var classId = UUID.randomUUID();
        var expected = new PageResult<SchoolClassUserResponse>(List.of(), 1, 20, 0, 0);
        var query = new ViewSchoolClassUsersQuery(classId, 1, 20);
        when(classUsersUseCase.execute(query)).thenReturn(expected);

        var result = controller.schoolClassUsers(classId, 1, 20);

        assertThat(result).isEqualTo(expected);
        verify(classUsersUseCase).execute(query);
    }

    @Test
    void school_class_users_should_throw_when_page_or_size_invalid() {
        var controller = new SchoolController(
            mock(ViewSchoolsUseCase.class),
            mock(ViewSchoolClassesUseCase.class),
            mock(ViewSchoolClassDetailsUseCase.class),
            mock(ViewSchoolClassUsersUseCase.class),
            mock(UpdateSchoolClassUseCase.class)
        );

        assertThrows(IllegalStateException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 0, 20));
        assertThrows(IllegalStateException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 1, 0));
    }

    @Test
    void update_school_class_name_only_should_return_id_response() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, classUsersUseCase, updateUseCase);
        var classId = UUID.randomUUID();
        var input = Map.<String, Object>of("name", "English 02");
        var command = new UpdateSchoolClassCommand(classId, "English 02", true, null, false, null, false);
        var expected = new UpdateSchoolClassResponse(classId);
        when(updateUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateUseCase).execute(command);
    }

    @Test
    void update_school_class_description_null_should_map_presence() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, classUsersUseCase, updateUseCase);
        var classId = UUID.randomUUID();
        var input = new HashMap<String, Object>();
        input.put("description", null);
        var command = new UpdateSchoolClassCommand(classId, null, false, null, true, null, false);
        var expected = new UpdateSchoolClassResponse(classId);
        when(updateUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateUseCase).execute(command);
    }

    @Test
    void update_school_class_status_only_should_map_presence() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, classUsersUseCase, updateUseCase);
        var classId = UUID.randomUUID();
        var input = Map.<String, Object>of("status", "INACTIVE");
        var command = new UpdateSchoolClassCommand(classId, null, false, null, false, "INACTIVE", true);
        var expected = new UpdateSchoolClassResponse(classId);
        when(updateUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateUseCase).execute(command);
    }

    private static SchoolClassResponse schoolClassResponse(UUID schoolId, UUID languageId, UUID gradeId) {
        return new SchoolClassResponse(
            UUID.randomUUID(),
            schoolId,
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class",
            "ACTIVE",
            "2026-06-06T12:00:00Z",
            "2026-06-06T12:00:00Z"
        );
    }
}
