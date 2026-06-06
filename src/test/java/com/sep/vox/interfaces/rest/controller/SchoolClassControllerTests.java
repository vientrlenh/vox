package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;

class SchoolClassControllerTests {

    @Test
    void create_should_return_created_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var controller = new SchoolClassController(createUseCase, deleteUseCase);
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var request = new CreateSchoolClassRequest(
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class"
        );
        var expectedCommand = new CreateSchoolClassCommand(
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class"
        );
        when(createUseCase.execute(expectedCommand)).thenReturn(new CreateSchoolClassResponse(schoolClassId));

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new CreateSchoolClassResponse(schoolClassId));
        verify(createUseCase).execute(expectedCommand);
    }

    @Test
    void delete_should_return_ok_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var controller = new SchoolClassController(createUseCase, deleteUseCase);
        var schoolClassId = UUID.randomUUID();
        var expected = new DeleteSchoolClassResponse(schoolClassId, "SOFT", "ARCHIVED", "2026-06-06T12:00:00Z");
        when(deleteUseCase.execute(new DeleteSchoolClassCommand(schoolClassId))).thenReturn(expected);

        var response = controller.delete(schoolClassId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(deleteUseCase).execute(new DeleteSchoolClassCommand(schoolClassId));
    }
}
