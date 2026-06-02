package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.schooladmin.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.ViewSchoolClassesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;

class SchoolClassControllerTests {

    @Test
    void school_classes_should_return_page_result() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var controller = new SchoolClassController(useCase, detailsUseCase);
        var expected = new PageResult<SchoolClassDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(new ViewSchoolClassesQuery(1, 20))).thenReturn(expected);

        var result = controller.schoolClasses(1, 20);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(new ViewSchoolClassesQuery(1, 20));
    }

    @Test
    void school_classes_should_throw_when_page_or_size_invalid() {
        var controller = new SchoolClassController(
            mock(ViewSchoolClassesUseCase.class),
            mock(ViewSchoolClassDetailsUseCase.class)
        );

        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(0, 20));
        assertThrows(IllegalStateException.class, () -> controller.schoolClasses(1, 0));
    }

    @Test
    void school_class_should_return_details() {
        var useCase = mock(ViewSchoolClassesUseCase.class);
        var detailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var controller = new SchoolClassController(useCase, detailsUseCase);
        var id = UUID.randomUUID();
        var dto = new SchoolClassDto(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG_10_A",
            "English 10A",
            "Test class",
            UUID.randomUUID(),
            "ACTIVE",
            "2026-06-02T00:00:00Z",
            "2026-06-02T00:00:00Z",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        when(detailsUseCase.execute(new ViewSchoolClassDetailsQuery(id))).thenReturn(dto);

        var result = controller.schoolClass(id);

        assertThat(result).isEqualTo(dto);
        verify(detailsUseCase).execute(new ViewSchoolClassDetailsQuery(id));
    }
}
