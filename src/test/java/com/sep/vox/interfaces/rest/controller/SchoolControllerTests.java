package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.AcceptSchoolGradeLevelImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.PreviewSchoolGradeLevelImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.school.CreateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.PreviewSchoolGradeImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.AcceptSchoolGradeImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.CreateSchoolGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.DeleteSchoolGradeLevelUseCase;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.application.port.input.usecase.schoolclassuser.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.BulkCreateSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.CreateSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.DeleteSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.PreviewSchoolClassUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.UpdateSchoolClassUserStatusUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.AcceptSchoolDirectoryImportUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.CreateSchoolDirectoryUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.PreviewSchoolDirectoryImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.VerifySchoolDirectoryUseCase;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.schoolclassuser.CreateSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclassuser.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassUserRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassUserStatusRequest;

class SchoolControllerTests {

    private CreateSchoolClassUseCase createSchoolClassUseCase;
    private DeleteSchoolClassUseCase deleteSchoolClassUseCase;
    private PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase;
    private AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase;
    private CreateSchoolUserUseCase createSchoolUserUseCase;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase;
    private CreateSchoolClassUserUseCase createSchoolClassUserUseCase;
    private BulkCreateSchoolClassUsersUseCase bulkCreateSchoolClassUsersUseCase;
    private DeleteSchoolClassUserUseCase deleteSchoolClassUserUseCase;
    private UpdateSchoolClassUserStatusUseCase updateSchoolClassUserStatusUseCase;
    private PreviewSchoolClassUserImportFromFileUseCase previewSchoolClassUserImportFromFileUseCase;
    private AcceptSchoolClassUserImportUseCase acceptSchoolClassUserImportUseCase;
    private CreateSchoolUseCase createSchoolUseCase;
    private DeleteSchoolUseCase deleteSchoolUseCase;
    private UpdateSchoolStatusUseCase updateSchoolStatusUseCase;
    private AddSchoolRoomUseCase addSchoolRoomUseCase;
    private DeleteSchoolRoomUseCase deleteSchoolRoomUseCase;
    private com.sep.vox.application.port.input.usecase.schoolroom.PreviewSchoolRoomImportFromFileUseCase previewSchoolRoomImportFromFileUseCase;
    private com.sep.vox.application.port.input.usecase.schoolroom.AcceptSchoolRoomImportUseCase acceptSchoolRoomImportUseCase;
    private CreateSchoolGradeUseCase createSchoolGradeUseCase;
    private DeleteSchoolGradeUseCase deleteSchoolGradeUseCase;
    private PreviewSchoolGradeImportFromFileUseCase previewSchoolGradeImportFromFileUseCase;
    private AcceptSchoolGradeImportUseCase acceptSchoolGradeImportUseCase;
    private CreateSchoolGradeLevelUseCase createSchoolGradeLevelUseCase;
    private DeleteSchoolGradeLevelUseCase deleteSchoolGradeLevelUseCase;
    private PreviewSchoolGradeLevelImportFromFileUseCase previewSchoolGradeLevelImportFromFileUseCase;
    private AcceptSchoolGradeLevelImportUseCase acceptSchoolGradeLevelImportUseCase;
    private PreviewSchoolDirectoryImportFromFileUseCase previewSchoolDirectoryImportFromFileUseCase;
    private AcceptSchoolDirectoryImportUseCase acceptSchoolDirectoryImportUseCase;
    private CreateSchoolDirectoryUseCase createSchoolDirectoryUseCase;
    private VerifySchoolDirectoryUseCase verifySchoolDirectoryUseCase;
    private SchoolController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID schoolClassId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID importSessionId = UUID.randomUUID();
    private final UUID languageId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createSchoolClassUseCase = mock(CreateSchoolClassUseCase.class);
        deleteSchoolClassUseCase = mock(DeleteSchoolClassUseCase.class);
        previewSchoolClassImportFromFileUseCase = mock(PreviewSchoolClassImportFromFileUseCase.class);
        acceptSchoolClassImportUseCase = mock(AcceptSchoolClassImportUseCase.class);
        createSchoolUserUseCase = mock(CreateSchoolUserUseCase.class);
        deleteSchoolUserUseCase = mock(DeleteSchoolUserUseCase.class);
        previewSchoolUserImportFromFileUseCase = mock(PreviewSchoolUserImportFromFileUseCase.class);
        acceptSchoolUserImportUseCase = mock(AcceptSchoolUserImportUseCase.class);
        createSchoolClassUserUseCase = mock(CreateSchoolClassUserUseCase.class);
        bulkCreateSchoolClassUsersUseCase = mock(BulkCreateSchoolClassUsersUseCase.class);
        deleteSchoolClassUserUseCase = mock(DeleteSchoolClassUserUseCase.class);
        updateSchoolClassUserStatusUseCase = mock(UpdateSchoolClassUserStatusUseCase.class);
        previewSchoolClassUserImportFromFileUseCase = mock(PreviewSchoolClassUserImportFromFileUseCase.class);
        acceptSchoolClassUserImportUseCase = mock(AcceptSchoolClassUserImportUseCase.class);
        createSchoolUseCase = mock(CreateSchoolUseCase.class);
        deleteSchoolUseCase = mock(DeleteSchoolUseCase.class);
        updateSchoolStatusUseCase = mock(UpdateSchoolStatusUseCase.class);
        addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        deleteSchoolRoomUseCase = mock(DeleteSchoolRoomUseCase.class);
        previewSchoolRoomImportFromFileUseCase = mock(com.sep.vox.application.port.input.usecase.schoolroom.PreviewSchoolRoomImportFromFileUseCase.class);
        acceptSchoolRoomImportUseCase = mock(com.sep.vox.application.port.input.usecase.schoolroom.AcceptSchoolRoomImportUseCase.class);
        createSchoolGradeUseCase = mock(CreateSchoolGradeUseCase.class);
        deleteSchoolGradeUseCase = mock(DeleteSchoolGradeUseCase.class);
        previewSchoolGradeImportFromFileUseCase = mock(PreviewSchoolGradeImportFromFileUseCase.class);
        acceptSchoolGradeImportUseCase = mock(AcceptSchoolGradeImportUseCase.class);
        createSchoolGradeLevelUseCase = mock(CreateSchoolGradeLevelUseCase.class);
        deleteSchoolGradeLevelUseCase = mock(DeleteSchoolGradeLevelUseCase.class);
        previewSchoolGradeLevelImportFromFileUseCase = mock(PreviewSchoolGradeLevelImportFromFileUseCase.class);
        acceptSchoolGradeLevelImportUseCase = mock(AcceptSchoolGradeLevelImportUseCase.class);
        previewSchoolDirectoryImportFromFileUseCase = mock(PreviewSchoolDirectoryImportFromFileUseCase.class);
        acceptSchoolDirectoryImportUseCase = mock(AcceptSchoolDirectoryImportUseCase.class);
        createSchoolDirectoryUseCase = mock(CreateSchoolDirectoryUseCase.class);
        verifySchoolDirectoryUseCase = mock(VerifySchoolDirectoryUseCase.class);
        
        controller = new SchoolController(
            createSchoolClassUseCase, 
            createSchoolClassUserUseCase,
            bulkCreateSchoolClassUsersUseCase,
            deleteSchoolClassUseCase, 
            deleteSchoolClassUserUseCase, 
            updateSchoolClassUserStatusUseCase, 
            previewSchoolClassImportFromFileUseCase, 
            acceptSchoolClassImportUseCase,
            createSchoolUserUseCase, 
            deleteSchoolUserUseCase,
            previewSchoolUserImportFromFileUseCase,
            acceptSchoolUserImportUseCase,
            previewSchoolClassUserImportFromFileUseCase, 
            acceptSchoolClassUserImportUseCase,
            createSchoolUseCase,
            deleteSchoolUseCase,
            updateSchoolStatusUseCase,
            addSchoolRoomUseCase,
            deleteSchoolRoomUseCase,
            previewSchoolRoomImportFromFileUseCase,
            acceptSchoolRoomImportUseCase,
            createSchoolGradeUseCase,
            deleteSchoolGradeUseCase,
            previewSchoolGradeImportFromFileUseCase,
            acceptSchoolGradeImportUseCase,
            createSchoolGradeLevelUseCase,
            deleteSchoolGradeLevelUseCase,
            previewSchoolGradeLevelImportFromFileUseCase,
            acceptSchoolGradeLevelImportUseCase,
            previewSchoolDirectoryImportFromFileUseCase,
            acceptSchoolDirectoryImportUseCase, 
            createSchoolDirectoryUseCase,
            verifySchoolDirectoryUseCase
        );
    }

    @Test
    void create_school_class_should_return_created_response() {
        var request = new CreateSchoolClassRequest(
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class"
        );
        var expectedCommand = new CreateSchoolClassCommand(
            schoolId,
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class"
        );
        when(createSchoolClassUseCase.execute(expectedCommand)).thenReturn(new CreateSchoolClassResponse(schoolClassId));

        var response = controller.create(schoolId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new CreateSchoolClassResponse(schoolClassId));
        verify(createSchoolClassUseCase).execute(expectedCommand);
    }

    @Test
    void create_class_user_should_return_created_response() {
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var schoolClassUserId = UUID.randomUUID();
        var request = new CreateSchoolClassUserRequest(userId);
        var expectedCommand = new CreateSchoolClassUserCommand(schoolId, classId, userId);
        var expected = new CreateSchoolClassUserResponse(schoolClassUserId);
        when(createSchoolClassUserUseCase.execute(any(CreateSchoolClassUserCommand.class))).thenReturn(expected);

        var response = controller.createClassUser(schoolId, classId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(createSchoolClassUserUseCase).execute(expectedCommand);
    }

    @Test
    void delete_school_class_should_return_ok_response() {
        var schoolId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var response = controller.delete(schoolId, schoolClassId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Xóa lớp học thành công");
        assertThat(response.getBody().data()).isNull();
        verify(deleteSchoolClassUseCase).execute(new DeleteSchoolClassCommand(schoolId, schoolClassId));
    }

    @Test
    void create_import_file_session_should_return_preview_response() throws Exception {
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
        when(previewSchoolClassImportFromFileUseCase.execute(any(PreviewSchoolClassImportFromFileCommand.class))).thenReturn(expected);

        var response = controller.createImportFileSession(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewSchoolClassImportFromFileUseCase).execute(any(PreviewSchoolClassImportFromFileCommand.class));
    }

    @Test
    void accept_school_class_import_session_should_return_ok() {
        var request = new AcceptSchoolClassImportRequest(Map.of(
            "Mã lớp", "code",
            "Tên lớp", "name",
            "Ngôn ngữ", "languageCode",
            "Khối", "schoolGradeCode"
        ));
        var expectedCommand = new AcceptSchoolClassImportCommand(schoolId, importSessionId, request.confirmedMapping());

        var response = controller.acceptImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Yêu cầu import lớp học đã được tiếp nhận, đang xử lý");
        verify(acceptSchoolClassImportUseCase).execute(expectedCommand);
    }

    @Test
    void create_user_should_return_created_with_response() {
        var request = new CreateSchoolUserRequest(
            "student@school.edu.vn", "0987654321", "John Cena",
            "2005-01-15", "123 Street", "STUDENT", null, null
        );
        var expected = new CreateSchoolUserResponse(userId);
        when(createSchoolUserUseCase.execute(any(CreateSchoolUserCommand.class))).thenReturn(expected);

        var response = controller.createUser(schoolId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Tạo người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(createSchoolUserUseCase).execute(any(CreateSchoolUserCommand.class));
    }

    @Test
    void delete_school_user_should_return_no_content() {
        var response = controller.deleteUser(schoolId, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(deleteSchoolUserUseCase).execute(new DeleteSchoolUserCommand(schoolId, userId));
    }

    @Test
    void preview_import_user_should_return_ok() throws Exception {
        var file = new MockMultipartFile("file", "students.csv", "text/csv", "email,fullName\n".getBytes());
        var expected = new PreviewSchoolUserImportResponse(
            importSessionId, "students.csv",
            List.of("email", "fullName"),
            Map.of("email", "email"),
            List.of(), 1L,
            OffsetDateTime.now().plusDays(1).toString()
        );
        when(previewSchoolUserImportFromFileUseCase.execute(any())).thenReturn(expected);

        var response = controller.previewImportFile(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Preview import người dùng thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewSchoolUserImportFromFileUseCase).execute(any());
    }

    @Test
    void accept_import_user_should_return_ok() {
        var request = new AcceptSchoolUserImportRequest(Map.of("Email", "email", "Họ tên", "fullName"));
        var response = controller.acceptImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Yêu cầu import người dùng đã được tiếp nhận, đang xử lý");
        verify(acceptSchoolUserImportUseCase).execute(any(AcceptSchoolUserImportCommand.class));
    }

    @Test
    void create_class_user_import_file_session_should_return_preview_response() throws Exception {
        var expected = new PreviewSchoolClassUserImportResponse(
            importSessionId,
            "class-users.csv",
            List.of("email", "classCode"),
            Map.of("email", "email", "classCode", "classCode"),
            List.of(Map.of("email", "student@example.com", "classCode", "ENG-01")),
            1L,
            "2026-06-09T10:00:00+07:00"
        );
        var file = new MockMultipartFile(
            "file",
            "class-users.csv",
            "text/csv",
            "email,classCode\nstudent@example.com,ENG-01\n".getBytes(StandardCharsets.UTF_8)
        );
        when(previewSchoolClassUserImportFromFileUseCase.execute(any(PreviewSchoolClassUserImportFromFileCommand.class))).thenReturn(expected);

        var response = controller.createClassUserImportFileSession(schoolId, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(previewSchoolClassUserImportFromFileUseCase).execute(any(PreviewSchoolClassUserImportFromFileCommand.class));
    }

    @Test
    void accept_class_user_import_session_should_return_ok() {
        var request = new AcceptSchoolClassUserImportRequest(Map.of(
            "Email", "email",
            "Mã lớp", "classCode"
        ));

        var response = controller.acceptClassUserImportSession(schoolId, importSessionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Yêu cầu import người dùng vào lớp học đã được tiếp nhận, đang xử lý");
        verify(acceptSchoolClassUserImportUseCase).execute(any(AcceptSchoolClassUserImportCommand.class));
    }

    @Test
    void deleteClassUser_should_return_ok_response_without_success_field() {
        var expectedCommand = new DeleteSchoolClassUserCommand(schoolId, schoolClassId, userId);
        var response = controller.deleteClassUser(schoolId, schoolClassId, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Xóa người dùng khỏi lớp học thành công");
        assertThat(response.getBody().data()).isNull();
        verify(deleteSchoolClassUserUseCase).execute(expectedCommand);
    }

    @Test
    void update_class_user_status_should_return_ok_response_without_success_field() {
        var request = new UpdateSchoolClassUserStatusRequest(true);
        var expected = new UpdateSchoolClassUserStatusResponse(schoolClassId);

        when(updateSchoolClassUserStatusUseCase.execute(
                any(UpdateSchoolClassUserStatusCommand.class))).thenReturn(expected);

        var response = controller.updateClassUserStatus(schoolId, schoolClassId, userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Cập nhật trạng thái người dùng trong lớp học thành công");
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(updateSchoolClassUserStatusUseCase).execute(any(UpdateSchoolClassUserStatusCommand.class));
    }
}
