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
import com.sep.vox.application.port.input.command.ImportSchoolClassesCommand;
import com.sep.vox.application.port.input.usecase.schooladmin.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.ImportSchoolClassesUseCase;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassImportResultDto;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;

public class SchoolClassControllerTests {

    @Test
    void create_should_return_created_response() {
        var createSchoolClassUseCase = mock(CreateSchoolClassUseCase.class);
        var importSchoolClassesUseCase = mock(ImportSchoolClassesUseCase.class);
        var controller = new SchoolClassController(createSchoolClassUseCase, importSchoolClassesUseCase);
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
        var dto = new SchoolClassDto(
            schoolClassId,
            schoolId,
            languageId,
            schoolGradeId,
            request.code(),
            request.name(),
            request.description(),
            targetSchoolLevelVersionId,
            "ACTIVE",
            "2026-06-01T00:00:00Z",
            "2026-06-01T00:00:00Z",
            UUID.randomUUID(),
            UUID.randomUUID()
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
        var controller = new SchoolClassController(createSchoolClassUseCase, importSchoolClassesUseCase);
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
}
