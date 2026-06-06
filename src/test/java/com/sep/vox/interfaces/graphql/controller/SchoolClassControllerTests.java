package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.response.input.schoolclass.SchoolClassResponse;
import com.sep.vox.domain.common.PageResult;

class SchoolClassControllerTests {

    @Test
    void school_classes_should_return_page_result() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var controller = new SchoolClassController(useCase, detailsUseCase);
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
        var controller = new SchoolClassController(
            mock(ViewSchoolClassesUseCase.class),
            mock(ViewSchoolClassDetailsUseCase.class)
        );

        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(0, 20, null, null, null, null));
        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(1, 0, null, null, null, null));
    }
}
