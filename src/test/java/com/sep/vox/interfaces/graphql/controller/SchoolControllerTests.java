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

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.response.input.schoolclass.SchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.domain.common.PageResult;

class SchoolControllerTests {

    @Test
    void school_classes_should_return_page_result() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, updateUseCase);
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
            mock(UpdateSchoolClassUseCase.class)
        );

        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(0, 20, null, null, null, null));
        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(1, 0, null, null, null, null));
    }

    @Test
    void school_class_should_return_details() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, updateUseCase);
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
    void update_school_class_name_only_should_return_id_response() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, updateUseCase);
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
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, updateUseCase);
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
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var controller = new SchoolController(mock(ViewSchoolsUseCase.class), useCase, detailsUseCase, updateUseCase);
        var classId = UUID.randomUUID();
        var input = Map.<String, Object>of("status", "INACTIVE");
        var command = new UpdateSchoolClassCommand(classId, null, false, null, false, "INACTIVE", true);
        var expected = new UpdateSchoolClassResponse(classId);
        when(updateUseCase.execute(command)).thenReturn(expected);

        var result = controller.updateSchoolClass(classId, input);

        assertThat(result).isEqualTo(expected);
        verify(updateUseCase).execute(command);
    }
}
