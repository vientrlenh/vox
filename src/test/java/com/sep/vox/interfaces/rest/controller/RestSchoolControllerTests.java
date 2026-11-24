package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.command.schoolclass.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.command.schooluser.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.command.schoolclass.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.school.CreateSchoolCommand;
import com.sep.vox.application.port.input.command.schoolgrade.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.command.schooluser.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.command.schoolclass.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.DeleteSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.schooluser.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.command.schoolclass.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.application.port.input.usecase.gradelevel.CreateGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.DeleteGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.school.CreateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
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
import com.sep.vox.application.port.input.usecase.schoolgrade.AcceptSchoolGradeImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.PreviewSchoolGradeImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AcceptSchoolRoomImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.PreviewSchoolRoomImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclassuser.CreateSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclassuser.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.support.RestControllerTest;

/**
 * The school routes are split between SCHOOL_ADMIN (everything under {@code /{schoolId}/...}) and
 * SYSTEM_ADMIN (the directories, grade levels and the school itself). The default user is a
 * SCHOOL_ADMIN; tests for the other group switch the role with {@code @WithMockUser}.
 */
@RestControllerTest(RestSchoolController.class)
@WithMockUser(roles = "SCHOOL_ADMIN")
@MockitoBean(types = {
    CreateSchoolClassUseCase.class, CreateSchoolClassUserUseCase.class, BulkCreateSchoolClassUsersUseCase.class,
    DeleteSchoolClassUseCase.class, DeleteSchoolClassUserUseCase.class, UpdateSchoolClassUserStatusUseCase.class,
    PreviewSchoolClassImportFromFileUseCase.class, AcceptSchoolClassImportUseCase.class,
    CreateSchoolUserUseCase.class, DeleteSchoolUserUseCase.class, PreviewSchoolUserImportFromFileUseCase.class,
    AcceptSchoolUserImportUseCase.class, PreviewSchoolClassUserImportFromFileUseCase.class,
    AcceptSchoolClassUserImportUseCase.class, CreateSchoolUseCase.class, DeleteSchoolUseCase.class,
    UpdateSchoolStatusUseCase.class, AddSchoolRoomUseCase.class, DeleteSchoolRoomUseCase.class,
    PreviewSchoolRoomImportFromFileUseCase.class, AcceptSchoolRoomImportUseCase.class,
    CreateSchoolGradeUseCase.class, DeleteSchoolGradeUseCase.class, PreviewSchoolGradeImportFromFileUseCase.class,
    AcceptSchoolGradeImportUseCase.class, CreateGradeLevelUseCase.class, DeleteGradeLevelUseCase.class,
    PreviewSchoolDirectoryImportFromFileUseCase.class, AcceptSchoolDirectoryImportUseCase.class,
    CreateSchoolDirectoryUseCase.class, VerifySchoolDirectoryUseCase.class
})
class RestSchoolControllerTests {

    private static final String SCHOOLS_URI = "/api/v1/schools";

    private final UUID schoolId = UUID.randomUUID();
    private final UUID schoolClassId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID importSessionId = UUID.randomUUID();
    private final UUID languageId = UUID.randomUUID();
    private final UUID gradeId = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CreateSchoolClassUseCase createSchoolClassUseCase;
    @Autowired
    private CreateSchoolClassUserUseCase createSchoolClassUserUseCase;
    @Autowired
    private DeleteSchoolClassUseCase deleteSchoolClassUseCase;
    @Autowired
    private DeleteSchoolClassUserUseCase deleteSchoolClassUserUseCase;
    @Autowired
    private UpdateSchoolClassUserStatusUseCase updateSchoolClassUserStatusUseCase;
    @Autowired
    private PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase;
    @Autowired
    private AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase;
    @Autowired
    private CreateSchoolUserUseCase createSchoolUserUseCase;
    @Autowired
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    @Autowired
    private PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    @Autowired
    private AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase;
    @Autowired
    private PreviewSchoolClassUserImportFromFileUseCase previewSchoolClassUserImportFromFileUseCase;
    @Autowired
    private AcceptSchoolClassUserImportUseCase acceptSchoolClassUserImportUseCase;
    @Autowired
    private CreateSchoolDirectoryUseCase createSchoolDirectoryUseCase;
    @Autowired
    private DeleteSchoolUseCase deleteSchoolUseCase;
    @Autowired
    private CreateSchoolUseCase createSchoolUseCase;
    @Autowired
    private CreateSchoolGradeUseCase createSchoolGradeUseCase;

    private MockMvcTester.MockMvcRequestBuilder postJson(String uri, String body, Object... uriVariables) {
        return mvc.post().uri(uri, uriVariables).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockMultipartFile csvFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void create_school_class_should_return_created_response() {
        CreateSchoolClassCommand expectedCommand = new CreateSchoolClassCommand(
            schoolId, languageId, gradeId, "ENG-01", "English 01", "Starter class");
        when(createSchoolClassUseCase.execute(expectedCommand)).thenReturn(new CreateSchoolClassResponse(schoolClassId));

        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/classes", """
            {"languageId": "%s", "schoolGradeId": "%s", "code": "ENG-01", "name": "English 01",
             "description": "Starter class"}""".formatted(languageId, gradeId), schoolId).exchange();

        assertThat(result).hasStatus(201).bodyJson()
            .extractingPath("$.data.schoolClassId").isEqualTo(schoolClassId.toString());
        verify(createSchoolClassUseCase).execute(expectedCommand);
    }

    @Test
    void create_school_class_should_return_bad_request_when_code_is_missing() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/classes",
            "{\"languageId\": \"%s\", \"schoolGradeId\": \"%s\", \"name\": \"English 01\"}".formatted(languageId, gradeId),
            schoolId).exchange();

        assertThat(result).hasStatus(400);
        verifyNoInteractions(createSchoolClassUseCase);
    }

    @Test
    void create_class_user_should_return_created_response() {
        UUID schoolClassUserId = UUID.randomUUID();
        CreateSchoolClassUserCommand expectedCommand = new CreateSchoolClassUserCommand(schoolId, schoolClassId, userId);
        when(createSchoolClassUserUseCase.execute(expectedCommand))
            .thenReturn(new CreateSchoolClassUserResponse(schoolClassUserId));

        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/classes/{classId}/users",
            "{\"userId\": \"%s\"}".formatted(userId), schoolId, schoolClassId).exchange();

        assertThat(result).hasStatus(201).bodyJson()
            .extractingPath("$.data.schoolClassUserId").isEqualTo(schoolClassUserId.toString());
    }

    @Test
    void delete_school_class_should_return_ok_response() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{schoolId}/classes/{classId}", schoolId, schoolClassId)
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Xóa lớp học thành công");
        verify(deleteSchoolClassUseCase).execute(new DeleteSchoolClassCommand(schoolId, schoolClassId));
    }

    @Test
    void create_import_file_session_should_return_preview_response() {
        PreviewSchoolClassImportResponse expected = new PreviewSchoolClassImportResponse(importSessionId, "classes.csv",
            List.of("code", "name"), Map.of("code", "code", "name", "name"),
            List.of(Map.of("code", "ENG-01", "name", "English 01")), 1L, "2026-06-09T10:00:00+07:00");
        when(previewSchoolClassImportFromFileUseCase.execute(any(PreviewSchoolClassImportFromFileCommand.class)))
            .thenReturn(expected);

        MvcTestResult result = mvc.post().uri(SCHOOLS_URI + "/{schoolId}/classes/import/preview", schoolId)
            .multipart().file(csvFile("classes.csv", "code,name\nENG-01,English 01\n")).exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.data.importSessionId").isEqualTo(importSessionId.toString());
    }

    @Test
    void accept_school_class_import_session_should_return_ok() {
        Map<String, String> mapping = Map.of("Mã lớp", "code", "Tên lớp", "name", "Ngôn ngữ", "languageCode", "Khối", "schoolGradeCode");

        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/classes/import/{sessionId}/accept", """
            {"confirmedMapping": {"Mã lớp": "code", "Tên lớp": "name", "Ngôn ngữ": "languageCode",
                                  "Khối": "schoolGradeCode"}}""", schoolId, importSessionId).exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Yêu cầu import lớp học đã được tiếp nhận, đang xử lý");
        verify(acceptSchoolClassImportUseCase)
            .execute(new AcceptSchoolClassImportCommand(schoolId, importSessionId, mapping));
    }

    @Test
    void accept_school_class_import_session_should_return_bad_request_when_mapping_is_empty() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/classes/import/{sessionId}/accept",
            "{\"confirmedMapping\": {}}", schoolId, importSessionId).exchange();

        assertThat(result).hasStatus(400);
        verifyNoInteractions(acceptSchoolClassImportUseCase);
    }

    @Test
    void create_user_should_return_created_with_response() {
        CreateSchoolUserResponse expected = new CreateSchoolUserResponse(userId);
        when(createSchoolUserUseCase.execute(any(CreateSchoolUserCommand.class))).thenReturn(expected);

        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/users", """
            {"email": "student@school.edu.vn", "phone": "0987654321", "fullName": "John Cena",
             "dateOfBirth": "2005-01-15", "address": "123 Street", "roleCode": "STUDENT"}""", schoolId).exchange();

        assertThat(result).hasStatus(201).bodyJson().extractingPath("$.message").isEqualTo("Tạo người dùng thành công");
        assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(userId.toString());
    }

    @Test
    void delete_school_user_should_return_no_content() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{schoolId}/users/{userId}", schoolId, userId).exchange();

        assertThat(result).hasStatus(204);
        verify(deleteSchoolUserUseCase).execute(new DeleteSchoolUserCommand(schoolId, userId));
    }

    @Test
    void preview_import_user_should_return_ok() {
        PreviewSchoolUserImportResponse expected = new PreviewSchoolUserImportResponse(importSessionId, "students.csv",
            List.of("email", "fullName"), Map.of("email", "email"), List.of(), 1L,
            OffsetDateTime.now().plusDays(1).toString());
        when(previewSchoolUserImportFromFileUseCase.execute(any())).thenReturn(expected);

        MvcTestResult result = mvc.post().uri(SCHOOLS_URI + "/{schoolId}/users/import/preview", schoolId)
            .multipart().file(csvFile("students.csv", "email,fullName\n")).exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Preview import người dùng thành công");
    }

    @Test
    void accept_import_user_should_return_ok() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/users/import/{sessionId}/accept",
            "{\"confirmedMapping\": {\"Email\": \"email\", \"Họ tên\": \"fullName\"}}", schoolId, importSessionId)
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Yêu cầu import người dùng đã được tiếp nhận, đang xử lý");
        verify(acceptSchoolUserImportUseCase).execute(any(AcceptSchoolUserImportCommand.class));
    }

    @Test
    void create_class_user_import_file_session_should_return_preview_response() {
        PreviewSchoolClassUserImportResponse expected = new PreviewSchoolClassUserImportResponse(importSessionId, "class-users.csv",
            List.of("email", "classCode"), Map.of("email", "email", "classCode", "classCode"),
            List.of(Map.of("email", "student@example.com", "classCode", "ENG-01")), 1L, "2026-06-09T10:00:00+07:00");
        when(previewSchoolClassUserImportFromFileUseCase.execute(any(PreviewSchoolClassUserImportFromFileCommand.class)))
            .thenReturn(expected);

        MvcTestResult result = mvc.post().uri(SCHOOLS_URI + "/{schoolId}/classes/users/import/preview", schoolId)
            .multipart().file(csvFile("class-users.csv", "email,classCode\nstudent@example.com,ENG-01\n")).exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.data.importSessionId").isEqualTo(importSessionId.toString());
    }

    @Test
    void accept_class_user_import_session_should_return_ok() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/classes/users/import/{sessionId}/accept",
            "{\"confirmedMapping\": {\"Email\": \"email\", \"Mã lớp\": \"classCode\"}}", schoolId, importSessionId)
            .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.message")
            .isEqualTo("Yêu cầu import người dùng vào lớp học đã được tiếp nhận, đang xử lý");
        verify(acceptSchoolClassUserImportUseCase).execute(any(AcceptSchoolClassUserImportCommand.class));
    }

    @Test
    void delete_class_user_should_return_ok_response_without_data() {
        MvcTestResult result = mvc.delete()
            .uri(SCHOOLS_URI + "/{schoolId}/classes/{classId}/users/{userId}", schoolId, schoolClassId, userId)
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Xóa người dùng khỏi lớp học thành công");
        assertThat(result).bodyJson().extractingPath("$.data").isNull();
        verify(deleteSchoolClassUserUseCase).execute(new DeleteSchoolClassUserCommand(schoolId, schoolClassId, userId));
    }

    @Test
    void update_class_user_status_should_return_ok_response() {
        UpdateSchoolClassUserStatusResponse expected = new UpdateSchoolClassUserStatusResponse(schoolClassId);
        when(updateSchoolClassUserStatusUseCase.execute(any(UpdateSchoolClassUserStatusCommand.class)))
            .thenReturn(expected);

        MvcTestResult result = mvc.patch()
            .uri(SCHOOLS_URI + "/{schoolId}/classes/{classId}/users/{userId}/status", schoolId, schoolClassId, userId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"isActive\": true}").exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.message")
            .isEqualTo("Cập nhật trạng thái người dùng trong lớp học thành công");
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void school_admin_routes_should_return_403_for_system_admin() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{schoolId}/classes/{classId}", schoolId, schoolClassId)
            .exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(deleteSchoolClassUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void school_admin_routes_should_return_403_for_teacher() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{schoolId}/users/{userId}", schoolId, userId).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(deleteSchoolUserUseCase);
    }

    @Test
    void system_admin_routes_should_return_403_for_school_admin() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{id}", schoolId).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(deleteSchoolUseCase);
    }

    /**
     * The body is valid on purpose: {@code @Valid} runs before {@code @PreAuthorize}, so an invalid body
     * would be answered with 400 and never reach the role check.
     */
    @Test
    void create_school_directory_should_return_403_for_school_admin() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/directories", """
            {"code": "HCM-01", "name": "Vox HCM", "provinceCode": "79", "provinceName": "Ho Chi Minh",
             "districtName": "District 1"}""").exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(createSchoolDirectoryUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void delete_school_should_return_ok_for_system_admin() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{id}", schoolId).exchange();

        assertThat(result).hasStatusOk();
        verify(deleteSchoolUseCase).execute(any());
    }

    @Test
    @WithAnonymousUser
    void school_admin_routes_should_return_401_without_authentication() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{schoolId}/classes/{classId}", schoolId, schoolClassId)
            .exchange();

        assertThat(result).hasStatus(401);
        verifyNoInteractions(deleteSchoolClassUseCase);
    }

    @Test
    @WithAnonymousUser
    void system_admin_routes_should_return_401_without_authentication() {
        MvcTestResult result = mvc.delete().uri(SCHOOLS_URI + "/{id}", schoolId).exchange();

        assertThat(result).hasStatus(401);
        verifyNoInteractions(deleteSchoolUseCase);
    }

    @Test
    void create_user_should_pass_iso_dates_to_the_command() {
        postJson(SCHOOLS_URI + "/{schoolId}/users", """
            {"email": "student@school.edu.vn", "phone": "0987654321", "fullName": "John Cena",
             "dateOfBirth": "2005-01-15", "roleCode": "STUDENT", "startDate": "2025-09-05", "endDate": "2026-05-31"}""",
            schoolId).exchange();

        verify(createSchoolUserUseCase).execute(new CreateSchoolUserCommand(schoolId, "student@school.edu.vn",
            "0987654321", "John Cena", LocalDate.of(2005, 1, 15), null, "STUDENT",
            LocalDate.of(2025, 9, 5), LocalDate.of(2026, 5, 31)));
    }

    @Test
    void create_user_should_return_400_naming_the_field_for_a_non_iso_date_of_birth() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/users", """
            {"email": "student@school.edu.vn", "phone": "0987654321", "fullName": "John Cena",
             "dateOfBirth": "15/01/2005", "roleCode": "STUDENT"}""", schoolId).exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.dateOfBirth").isNotNull();
        verifyNoInteractions(createSchoolUserUseCase);
    }

    @Test
    void create_user_should_return_400_when_date_of_birth_is_missing() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/users", """
            {"email": "student@school.edu.vn", "phone": "0987654321", "fullName": "John Cena",
             "roleCode": "STUDENT"}""", schoolId).exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.dateOfBirth").isNotNull();
        verifyNoInteractions(createSchoolUserUseCase);
    }

    @Test
    void create_school_grade_should_pass_iso_dates_to_the_command() {
        UUID newGradeId = UUID.randomUUID();
        CreateSchoolGradeCommand expected = new CreateSchoolGradeCommand(schoolId, gradeId, "G10", "Grade 10", null,
            LocalDate.of(2026, 9, 5), LocalDate.of(2027, 5, 31));
        when(createSchoolGradeUseCase.execute(expected)).thenReturn(newGradeId);

        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/grade-levels/{gradeLevelId}/grades", """
            {"code": "G10", "name": "Grade 10", "startDate": "2026-09-05", "endDate": "2027-05-31"}""",
            schoolId, gradeId).exchange();

        assertThat(result).hasStatusOk();
        verify(createSchoolGradeUseCase).execute(expected);
    }

    @Test
    void create_school_grade_should_return_400_naming_the_field_for_an_impossible_date() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/grade-levels/{gradeLevelId}/grades", """
            {"code": "G10", "name": "Grade 10", "startDate": "31/02/2026", "endDate": "2027-05-31"}""",
            schoolId, gradeId).exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.startDate").isNotNull();
        verifyNoInteractions(createSchoolGradeUseCase);
    }

    @Test
    void create_school_grade_should_return_400_when_end_date_is_missing() {
        MvcTestResult result = postJson(SCHOOLS_URI + "/{schoolId}/grade-levels/{gradeLevelId}/grades", """
            {"code": "G10", "name": "Grade 10", "startDate": "2026-09-05"}""", schoolId, gradeId).exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.endDate").isNotNull();
        verifyNoInteractions(createSchoolGradeUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void create_school_should_pass_admin_date_of_birth_to_the_command() {
        UUID newSchoolId = UUID.randomUUID();
        CreateSchoolCommand expected = new CreateSchoolCommand(null, "S1", "School 1", null, null, 100,
            "admin@school.edu.vn", null, "Admin", LocalDate.of(1980, 5, 20), null, null);
        when(createSchoolUseCase.execute(expected)).thenReturn(newSchoolId);

        MvcTestResult result = postJson(SCHOOLS_URI, """
            {"schoolCode": "S1", "schoolName": "School 1", "studentCount": 100, "adminEmail": "admin@school.edu.vn",
             "adminFullName": "Admin", "adminDateOfBirth": "1980-05-20"}""").exchange();

        assertThat(result).hasStatus(201);
        verify(createSchoolUseCase).execute(expected);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void create_school_should_return_400_naming_the_field_for_a_non_iso_admin_date_of_birth() {
        MvcTestResult result = postJson(SCHOOLS_URI, """
            {"schoolCode": "S1", "schoolName": "School 1", "studentCount": 100, "adminEmail": "admin@school.edu.vn",
             "adminFullName": "Admin", "adminDateOfBirth": "20/05/1980"}""").exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.adminDateOfBirth").isNotNull();
        verifyNoInteractions(createSchoolUseCase);
    }
}
