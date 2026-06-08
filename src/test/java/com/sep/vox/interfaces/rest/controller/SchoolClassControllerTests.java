package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;

class SchoolClassControllerTests {

    @Test
    void create_should_return_created_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var controller = new SchoolClassController(createUseCase, deleteUseCase, previewUseCase, acceptUseCase);
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
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var controller = new SchoolClassController(createUseCase, deleteUseCase, previewUseCase, acceptUseCase);
        var schoolClassId = UUID.randomUUID();
        var expected = new DeleteSchoolClassResponse(schoolClassId, "SOFT", "ARCHIVED", "2026-06-06T12:00:00Z");
        when(deleteUseCase.execute(new DeleteSchoolClassCommand(schoolClassId))).thenReturn(expected);

        var response = controller.delete(schoolClassId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(deleteUseCase).execute(new DeleteSchoolClassCommand(schoolClassId));
    }

    @Test
    void createImportFileSession_should_return_preview_response() throws Exception {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var controller = new SchoolClassController(createUseCase, deleteUseCase, previewUseCase, acceptUseCase);
        var importSessionId = UUID.randomUUID();
        var expected = new PreviewSchoolClassImportResponse(
            importSessionId,
            "classes.csv",
            List.of("code", "name"),
            Map.of("code", "code", "name", "name"),
            List.of(Map.of("code", "ENG-01", "name", "English 01")),
            1L,
            "2026-06-09T10:00:00+07:00"
        );
        var file = new MockMultipartFile(
            "file",
            "classes.csv",
            "text/csv",
            "code,name\nENG-01,English 01\n".getBytes(StandardCharsets.UTF_8)
        );
        when(previewUseCase.execute(any(PreviewSchoolClassImportFromFileCommand.class))).thenReturn(expected);

        var response = controller.createImportFileSession(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewUseCase).execute(any(PreviewSchoolClassImportFromFileCommand.class));
    }

    @Test
    void acceptImportSession_should_return_accept_response() {
        var createUseCase = mock(CreateSchoolClassUseCase.class);
        var deleteUseCase = mock(DeleteSchoolClassUseCase.class);
        var previewUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        var acceptUseCase = mock(AcceptSchoolClassImportUseCase.class);
        var controller = new SchoolClassController(createUseCase, deleteUseCase, previewUseCase, acceptUseCase);
        var importSessionId = UUID.randomUUID();
        var request = new AcceptSchoolClassImportRequest(Map.of(
            "Mã lớp", "code",
            "Tên lớp", "name",
            "Ngôn ngữ", "languageCode",
            "Khối", "schoolGradeCode"
        ));
        var expected = new AcceptSchoolClassImportResponse(importSessionId, 2L, 1L, 1L, 0L, "COMPLETED");
        var expectedCommand = new AcceptSchoolClassImportCommand(importSessionId, request.confirmedMapping());
        when(acceptUseCase.execute(expectedCommand)).thenReturn(expected);

        var response = controller.acceptImportSession(importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(acceptUseCase).execute(expectedCommand);
    }
}
