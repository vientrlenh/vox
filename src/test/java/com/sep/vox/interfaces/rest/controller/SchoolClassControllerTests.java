package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;

class SchoolClassControllerTests {

    @Test
    void create_should_return_created_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var updateUseCase = mock(UpdateSchoolClassUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var controller = new SchoolClassController(createUseCase, updateUseCase, deleteUseCase);
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
}
