package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.ImportSchoolClassesCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schooladmin.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.ImportSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.UpdateSchoolClassUseCase;
import com.sep.vox.domain.dto.SchoolClassDeleteResultDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassImportResultDto;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassRequest;

public class SchoolClassControllerTests {

    @Test
    void create_should_return_created_response() {
        var createSchoolClassUseCase = mock(CreateSchoolClassUseCase.class);
        var importSchoolClassesUseCase = mock(ImportSchoolClassesUseCase.class);
        var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class);
        var deleteSchoolClassUseCase = mock(DeleteSchoolClassUseCase.class);
        var controller = new SchoolClassController(
            createSchoolClassUseCase,
            importSchoolClassesUseCase,
            updateSchoolClassUseCase,
            deleteSchoolClassUseCase
        );
        var languageId = UUID.randomUUID();
        var schoolGradeId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var targetSchoolLevelVersionId = UUID.randomUUID();
        var request = new CreateSchoolClassRequest(
            languageId,
            schoolGradeId,
            "ENG_10-A",
            "English 10A",
            "Optional description",
            targetSchoolLevelVersionId
        );
        var expectedCommand = new CreateSchoolClassCommand(
            languageId,
            schoolGradeId,
            request.code(),
            request.name(),
            request.description(),
            targetSchoolLevelVersionId
        );
        var dto = dto(
            schoolClassId,
            schoolId,
            languageId,
            schoolGradeId,
            request.code(),
            request.name(),
            request.description(),
            targetSchoolLevelVersionId,
            "ACTIVE"
        );
        when(createSchoolClassUseCase.execute(expectedCommand)).thenReturn(dto);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tạo lớp học thành công");
        assertThat(response.getBody().data()).isEqualTo(dto);
        verify(createSchoolClassUseCase).execute(expectedCommand);
    }

    @Test
    void import_file_should_return_created_response() {
        var createSchoolClassUseCase = mock(CreateSchoolClassUseCase.class);
        var importSchoolClassesUseCase = mock(ImportSchoolClassesUseCase.class);
        var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class);
        var deleteSchoolClassUseCase = mock(DeleteSchoolClassUseCase.class);
        var controller = new SchoolClassController(
            createSchoolClassUseCase,
            importSchoolClassesUseCase,
            updateSchoolClassUseCase,
            deleteSchoolClassUseCase
        );
        var file = new MockMultipartFile(
            "file",
            "classes.csv",
            "text/csv",
            """
            languageCode,schoolGradeCode,targetSchoolLevelCode,targetSchoolLevelVersion,code,name,description
            ENG,G10,A1,1,ENG_10_A,English 10A,Optional
            """.getBytes(StandardCharsets.UTF_8)
        );
        var result = new SchoolClassImportResultDto(1, 1, List.of());
        when(importSchoolClassesUseCase.execute(any(ImportSchoolClassesCommand.class))).thenReturn(result);

        var response = controller.importFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Import lớp học thành công");
        assertThat(response.getBody().data()).isEqualTo(result);
        verify(importSchoolClassesUseCase).execute(any(ImportSchoolClassesCommand.class));
    }

    @Test
    void update_should_return_ok_response() {
        var createSchoolClassUseCase = mock(CreateSchoolClassUseCase.class);
        var importSchoolClassesUseCase = mock(ImportSchoolClassesUseCase.class);
        var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class);
        var deleteSchoolClassUseCase = mock(DeleteSchoolClassUseCase.class);
        var controller = new SchoolClassController(
            createSchoolClassUseCase,
            importSchoolClassesUseCase,
            updateSchoolClassUseCase,
            deleteSchoolClassUseCase
        );
        var id = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var schoolGradeId = UUID.randomUUID();
        var targetSchoolLevelVersionId = UUID.randomUUID();
        var request = new UpdateSchoolClassRequest(
            "English 10A Updated",
            "Updated description",
            targetSchoolLevelVersionId,
            "INACTIVE"
        );
        var expectedCommand = new UpdateSchoolClassCommand(
            id,
            request.name(),
            request.description(),
            targetSchoolLevelVersionId,
            request.status()
        );
        var dto = dto(
            id,
            schoolId,
            languageId,
            schoolGradeId,
            "ENG_10_A",
            request.name(),
            request.description(),
            targetSchoolLevelVersionId,
            request.status()
        );
        when(updateSchoolClassUseCase.execute(expectedCommand)).thenReturn(dto);

        var response = controller.update(id, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Cập nhật lớp học thành công");
        assertThat(response.getBody().data()).isEqualTo(dto);
        verify(updateSchoolClassUseCase).execute(expectedCommand);
    }

    @Test
    void delete_should_return_ok_response() {
        var createSchoolClassUseCase = mock(CreateSchoolClassUseCase.class);
        var importSchoolClassesUseCase = mock(ImportSchoolClassesUseCase.class);
        var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class);
        var deleteSchoolClassUseCase = mock(DeleteSchoolClassUseCase.class);
        var controller = new SchoolClassController(
            createSchoolClassUseCase,
            importSchoolClassesUseCase,
            updateSchoolClassUseCase,
            deleteSchoolClassUseCase
        );
        var id = UUID.randomUUID();
        var expectedCommand = new DeleteSchoolClassCommand(id);
        var result = new SchoolClassDeleteResultDto(id, "HARD", null, null);
        when(deleteSchoolClassUseCase.execute(expectedCommand)).thenReturn(result);

        var response = controller.delete(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Xóa lớp học thành công");
        assertThat(response.getBody().data()).isEqualTo(result);
        verify(deleteSchoolClassUseCase).execute(expectedCommand);
    }

    private static SchoolClassDto dto(UUID id, UUID schoolId, UUID languageId, UUID schoolGradeId, String code,
            String name, String description, UUID targetSchoolLevelVersionId, String status) {
        return new SchoolClassDto(
            id,
            schoolId,
            languageId,
            schoolGradeId,
            code,
            name,
            description,
            targetSchoolLevelVersionId,
            status,
            "2026-06-01T00:00:00Z",
            "2026-06-01T00:00:00Z",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
