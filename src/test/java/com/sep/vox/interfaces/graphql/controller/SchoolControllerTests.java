// package com.sep.vox.interfaces.graphql.controller;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;
// import java.util.concurrent.CompletableFuture;

// import org.junit.jupiter.api.BeforeEach;
// import org.dataloader.DataLoader;
// import org.junit.jupiter.api.Test;

// import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
// import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
// import com.sep.vox.application.port.input.query.ViewSchoolUserDetailsQuery;
// import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
// import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
// import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;

// import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
// import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
// import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
// import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
// import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
// import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersBySchoolUseCase;
// import com.sep.vox.application.port.input.usecase.schooluser.UpdateSchoolUserUseCase;
// import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserDetailsUseCase;
// import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
// import com.sep.vox.domain.common.PageResult;
// import com.sep.vox.domain.dto.SchoolClassDto;
// import com.sep.vox.domain.dto.SchoolClassUserDto;
// import com.sep.vox.domain.dto.SchoolDto;
// import com.sep.vox.domain.dto.SchoolGradeDto;
// import com.sep.vox.domain.dto.SchoolUserDto;
// import com.sep.vox.domain.dto.SupportedLanguageDto;
// import com.sep.vox.domain.dto.UserDto;

// import graphql.schema.DataFetchingEnvironment;

// class SchoolControllerTests {

//     private ViewSchoolsUseCase viewSchoolsUseCase;
//     private ViewSchoolClassesUseCase viewSchoolClassesUseCase;
//     private ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase;
//     private UpdateSchoolClassUseCase updateSchoolClassUseCase;
//     private ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
//     private ViewSchoolUserDetailsUseCase viewSchoolUserDetailsUseCase;
//     private UpdateSchoolUserUseCase updateSchoolUserUseCase;
//     private SchoolController controller;

//     private final UUID schoolId = UUID.randomUUID();
//     private final UUID userId = UUID.randomUUID();

//     @BeforeEach
//     void setUp() {
//         viewSchoolsUseCase = mock(ViewSchoolsUseCase.class);
//         viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class);
//         viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         updateSchoolUserUseCase = mock(UpdateSchoolUserUseCase.class);

//         controller = new SchoolController(
//             viewSchoolsUseCase, viewSchoolClassesUseCase,
//             viewSchoolClassDetailsUseCase, updateSchoolClassUseCase,
//             viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase,
//             updateSchoolUserUseCase,
//             mock(com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase.class)
//         );
//     }

//     @Test
//     void school_classes_should_return_page_result() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var languageId = UUID.randomUUID();
//         var gradeId = UUID.randomUUID();
//         var expected = new PageResult<SchoolClassDto>(List.of(), 1, 20, 0, 0);
//         var query = new ViewSchoolClassesQuery(1, 20, "eng", "ACTIVE", languageId, gradeId);
//         when(viewSchoolClassesUseCase.execute(query)).thenReturn(expected);

//         var result = controller.schoolClasses(1, 20, "eng", "ACTIVE", languageId, gradeId);

//         assertThat(result).isEqualTo(expected);
//         verify(viewSchoolClassesUseCase).execute(query);
//     }

//     @Test
//     void school_classes_should_throw_when_page_or_size_invalid() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);

//         assertThrows(IllegalStateException.class, () -> controller.schoolClasses(0, 20, null, null, null, null));
//         assertThrows(IllegalStateException.class, () -> controller.schoolClasses(1, 0, null, null, null, null));
//     }

//     @Test
//     void school_class_should_return_details() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var classId = UUID.randomUUID();
//         var expected = new SchoolClassDto(
//             classId,
//             UUID.randomUUID(),
//             UUID.randomUUID(),
//             UUID.randomUUID(),
//             "ENG-01",
//             "English 01",
//             "Starter class",
//             "ACTIVE",
//             "2026-06-06T12:00:00Z",
//             "2026-06-06T12:00:00Z"
//         );
//         when(viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(classId))).thenReturn(expected);

//         var result = controller.schoolClass(classId);

//         assertThat(result).isEqualTo(expected);
//         verify(viewSchoolClassDetailsUseCase).execute(new ViewSchoolClassDetailsQuery(classId));
//     }

<<<<<<< HEAD
    @Test
    @SuppressWarnings("unchecked")
    void school_class_school_field_should_load_related_school() {
        var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
        var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
        var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
        var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
        var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
        var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
        var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
        updateSchoolUseCase, 
        classUsersUseCase);
        var schoolId = UUID.randomUUID();
        var response = schoolClassDto(schoolId, UUID.randomUUID(), UUID.randomUUID());
        var expected = new SchoolDto(schoolId, "SCH", "School", null, null, null, null, null, 0, true, null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, SchoolDto>getDataLoader("schoolByClass")).thenReturn(loader);
        when(loader.load(schoolId)).thenReturn(CompletableFuture.completedFuture(expected));
=======
//     @Test
//     @SuppressWarnings("unchecked")
//     void school_class_school_field_should_load_related_school() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var schoolId = UUID.randomUUID();
//         var response = schoolClassDto(schoolId, UUID.randomUUID(), UUID.randomUUID());
//         var expected = new SchoolDto(schoolId, "SCH", "School", null, null, null, null, null, 0, true, null, null);
//         var env = mock(DataFetchingEnvironment.class);
//         var loader = mock(DataLoader.class);
//         when(env.<UUID, SchoolDto>getDataLoader("schoolById")).thenReturn(loader);
//         when(loader.load(schoolId)).thenReturn(CompletableFuture.completedFuture(expected));
>>>>>>> default

//         var result = controller.school(response, env).join();

//         assertThat(result).isEqualTo(expected);
//         verify(loader).load(schoolId);
//     }

//     @Test
//     @SuppressWarnings("unchecked")
//     void school_class_school_grade_field_should_load_related_grade_with_school_scope() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var schoolId = UUID.randomUUID();
//         var gradeId = UUID.randomUUID();
//         var response = schoolClassDto(schoolId, UUID.randomUUID(), gradeId);
//         var expected = new SchoolGradeDto(gradeId, schoolId, "G10", "Grade 10", null, null, null, "ACTIVE", null, null);
//         var env = mock(DataFetchingEnvironment.class);
//         var loader = mock(DataLoader.class);;
//         when(env.<UUID, SchoolGradeDto>getDataLoader("schoolGradeByClass")).thenReturn(loader);
//         when(loader.load(gradeId)).thenReturn(CompletableFuture.completedFuture(expected));

//         var result = controller.schoolGrade(response, env).join();

//         assertThat(result).isEqualTo(expected);
//         verify(loader).load(gradeId);
//     }

<<<<<<< HEAD
    @Test
    @SuppressWarnings("unchecked")
    void school_class_language_field_should_load_related_language() {
        var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
        var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
        var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
        var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
        var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
        var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
        var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
        updateSchoolUseCase, 
        classUsersUseCase);
        var languageId = UUID.randomUUID();
        var response = schoolClassDto(UUID.randomUUID(), languageId, UUID.randomUUID());
        var expected = new SupportedLanguageDto(languageId, "EN", "English", null, true, null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, SupportedLanguageDto>getDataLoader("supportedLanguageByClass")).thenReturn(loader);
        when(loader.load(languageId)).thenReturn(CompletableFuture.completedFuture(expected));
=======
//     @Test
//     @SuppressWarnings("unchecked")
//     void school_class_language_field_should_load_related_language() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var languageId = UUID.randomUUID();
//         var response = schoolClassDto(UUID.randomUUID(), languageId, UUID.randomUUID());
//         var expected = new SupportedLanguageDto(languageId, "EN", "English", null, true, null, null);
//         var env = mock(DataFetchingEnvironment.class);
//         var loader = mock(DataLoader.class);
//         when(env.<UUID, SupportedLanguageDto>getDataLoader("supportedLanguageById")).thenReturn(loader);
//         when(loader.load(languageId)).thenReturn(CompletableFuture.completedFuture(expected));
>>>>>>> default

//         var result = controller.language(response, env).join();

//         assertThat(result).isEqualTo(expected);
//         verify(loader).load(languageId);
//     }

//     @Test
//     void school_class_users_should_return_page_result() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var classId = UUID.randomUUID();
//         var expected = new PageResult<SchoolClassUserDto>(List.of(), 1, 20, 0, 0);
//         var query = new ViewSchoolClassUsersQuery(classId, 1, 20);
//         when(classUsersUseCase.execute(query)).thenReturn(expected);

//         var result = controller.schoolClassUsers(classId, 1, 20);

//         assertThat(result).isEqualTo(expected);
//         verify(classUsersUseCase).execute(query);
//     }

//     @Test
//     void school_class_users_should_throw_when_page_or_size_invalid() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);

<<<<<<< HEAD
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 0, 20));
        assertThrows(IllegalArgumentException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 1, 0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void school_class_user_user_field_should_load_related_user() {
        var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
        var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
        var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
        var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
        var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
        var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
        var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
        var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
        var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
        updateSchoolUseCase, 
        classUsersUseCase);
        var userId = UUID.randomUUID();
        var response = new SchoolClassUserDto(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
            true,
            "2026-06-06T12:00:00Z",
            "2026-06-06T12:00:00Z",
            UUID.randomUUID()
        );
        var expected = new UserDto(userId, "student@example.com", null, "Student", null, null, null, null, null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, UserDto>getDataLoader("userBySchoolClassUser")).thenReturn(loader);
        when(loader.load(userId)).thenReturn(CompletableFuture.completedFuture(expected));
=======
//         assertThrows(IllegalStateException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 0, 20));
//         assertThrows(IllegalStateException.class, () -> controller.schoolClassUsers(UUID.randomUUID(), 1, 0));
//     }

//     @Test
//     @SuppressWarnings("unchecked")
//     void school_class_user_user_field_should_load_related_user() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var userId = UUID.randomUUID();
//         var response = new SchoolClassUserDto(
//             UUID.randomUUID(),
//             userId,
//             UUID.randomUUID(),
//             true,
//             "2026-06-06T12:00:00Z",
//             "2026-06-06T12:00:00Z"
//         );
//         var expected = new UserDto(userId, "student@example.com", null, "Student", null, null, null, null, null, null);
//         var env = mock(DataFetchingEnvironment.class);
//         var loader = mock(DataLoader.class);
//         when(env.<UUID, UserDto>getDataLoader("userById")).thenReturn(loader);
//         when(loader.load(userId)).thenReturn(CompletableFuture.completedFuture(expected));
>>>>>>> default

//         var result = controller.user(response, env).join();

//         assertThat(result).isEqualTo(expected);
//         verify(loader).load(userId);
//     }

//     @Test
//     void update_school_class_name_only_should_return_id_response() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var classId = UUID.randomUUID();
//         var input = Map.<String, Object>of("name", "English 02");
//         var command = new UpdateSchoolClassCommand(classId, "English 02", true, null, false, null, false);
//         var expected = new UpdateSchoolClassResponse(classId);
//         when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

//         var result = controller.updateSchoolClass(classId, input);

//         assertThat(result).isEqualTo(expected);
//         verify(updateSchoolClassUseCase).execute(command);
//     }

//     @Test
//     void update_school_class_description_null_should_map_presence() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var classId = UUID.randomUUID();
//         var input = new HashMap<String, Object>();
//         input.put("description", null);
//         var command = new UpdateSchoolClassCommand(classId, null, false, null, true, null, false);
//         var expected = new UpdateSchoolClassResponse(classId);
//         when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

//         var result = controller.updateSchoolClass(classId, input);

//         assertThat(result).isEqualTo(expected);
//         verify(updateSchoolClassUseCase).execute(command);
//     }

//     @Test
//     void update_school_class_status_only_should_map_presence() {
//         var viewSchoolUseCase = mock(ViewSchoolsUseCase.class);
//         var viewSchoolClassesUseCase = mock(ViewSchoolClassesUseCase.class);
//         var viewSchoolClassDetailsUseCase = mock(ViewSchoolClassDetailsUseCase.class);
//         var classUsersUseCase = mock(ViewSchoolClassUsersUseCase.class);
//         var updateSchoolClassUseCase = mock(UpdateSchoolClassUseCase.class); 
//         var viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
//         var viewSchoolUserDetailsUseCase = mock(ViewSchoolUserDetailsUseCase.class);
//         var updateSchoolUseCase = mock(UpdateSchoolUserUseCase.class);
//         var controller = new SchoolController(viewSchoolUseCase, viewSchoolClassesUseCase, viewSchoolClassDetailsUseCase, updateSchoolClassUseCase, viewSchoolUsersBySchoolUseCase, viewSchoolUserDetailsUseCase, 
//         updateSchoolUseCase, 
//         classUsersUseCase);
//         var classId = UUID.randomUUID();
//         var input = Map.<String, Object>of("status", "INACTIVE");
//         var command = new UpdateSchoolClassCommand(classId, null, false, null, false, "INACTIVE", true);
//         var expected = new UpdateSchoolClassResponse(classId);
//         when(updateSchoolClassUseCase.execute(command)).thenReturn(expected);

//         var result = controller.updateSchoolClass(classId, input);

//         assertThat(result).isEqualTo(expected);
//         verify(updateSchoolClassUseCase).execute(command);
//     }

<<<<<<< HEAD
    @Test
    void school_users_field_should_return_page_from_use_case() {
        var response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
        var page = new PageResult<>(List.of(response), 1, 20, 1, 1);
        when(viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null))).thenReturn(page);

        var result = controller.schoolUsersBySchool(schoolId, 1, 20, null, null, null);

        assertThat(result).isEqualTo(page);
        assertThat(result.content()).containsExactly(response);
        verify(viewSchoolUsersBySchoolUseCase).execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, null, null, null));
    }
=======
//     @Test
//     void school_users_field_should_return_page_from_use_case() {
//         var response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
//         var page = new PageResult<>(List.of(response), 1, 20, 1, 1);
//         when(viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20))).thenReturn(page);

//         var result = controller.schoolUsersBySchool(schoolId, 1, 20);

//         assertThat(result).isEqualTo(page);
//         assertThat(result.content()).containsExactly(response);
//         verify(viewSchoolUsersBySchoolUseCase).execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20));
//     }
>>>>>>> default

//     @Test
//     void school_users_by_school_field_should_return_details_from_use_case() {
//         var response = schoolUserDto(UUID.randomUUID(), userId, schoolId);
//         when(viewSchoolUserDetailsUseCase.execute(new ViewSchoolUserDetailsQuery(schoolId, userId))).thenReturn(response);

//         var result = controller.schoolUser(schoolId, userId);

//         assertThat(result).isEqualTo(response);
//         verify(viewSchoolUserDetailsUseCase).execute(new ViewSchoolUserDetailsQuery(schoolId, userId));
//     }

<<<<<<< HEAD
    @Test
    @SuppressWarnings("unchecked")
    void school_user_user_field_should_load_related_user() {
        var response = schoolUserDto(UUID.randomUUID(), schoolId, userId);
        var expected = new UserDto(userId, "student@example.com", null, "Student", null, null, null, null, null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, UserDto>getDataLoader("userBySchoolUser")).thenReturn(loader);
        when(loader.load(userId)).thenReturn(CompletableFuture.completedFuture(expected));

        var result = controller.schoolUserUser(response, env).join();

        assertThat(result).isEqualTo(expected);
        verify(loader).load(userId);
    }

    @Test
    void school_users_by_school_field_should_reject_invalid_paging() {
        assertThatThrownBy(() -> controller.schoolUsersBySchool(schoolId, 0, 20, null, null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
    }
=======
//     @Test
//     void school_users_by_school_field_should_reject_invalid_paging() {
//         assertThatThrownBy(() -> controller.schoolUsersBySchool(schoolId, 0, 20))
//             .isInstanceOf(IllegalStateException.class)
//             .hasMessage("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
//     }
>>>>>>> default

//     private SchoolUserDto schoolUserDto(UUID id, UUID schoolId, UUID userId) {
//         return new SchoolUserDto(id, schoolId, userId, null, null);
//     }

//     private static SchoolClassDto schoolClassDto(UUID schoolId, UUID languageId, UUID gradeId) {
//         return new SchoolClassDto(
//             UUID.randomUUID(),
//             schoolId,
//             languageId,
//             gradeId,
//             "ENG-01",
//             "English 01",
//             "Starter class",
//             "ACTIVE",
//             "2026-06-06T12:00:00Z",
//             "2026-06-06T12:00:00Z"
//         );
//     }
// }
