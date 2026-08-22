package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewMyClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewMyClassMembersQuery;
import com.sep.vox.application.port.input.query.ViewMyClassesQuery;
import com.sep.vox.application.port.input.query.key.UserRolesKey;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassMembersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;

import graphql.schema.DataFetchingEnvironment;

class MyClassControllerTests {

    private ViewMyClassesUseCase viewMyClassesUseCase;
    private ViewMyClassDetailsUseCase viewMyClassDetailsUseCase;
    private ViewMyClassMembersUseCase viewMyClassMembersUseCase;
    private MyClassController controller;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        viewMyClassesUseCase = mock(ViewMyClassesUseCase.class);
        viewMyClassDetailsUseCase = mock(ViewMyClassDetailsUseCase.class);
        viewMyClassMembersUseCase = mock(ViewMyClassMembersUseCase.class);
        controller = new MyClassController(
            viewMyClassesUseCase,
            viewMyClassDetailsUseCase,
            viewMyClassMembersUseCase
        );
    }

    @Test
    void my_classes_should_forward_arguments_to_use_case() {
        var expected = new PageResult<>(List.of(schoolClassDto()), 1, 10, 1, 1);
        when(viewMyClassesUseCase.execute(new ViewMyClassesQuery(schoolId, "eng", "ACTIVE", 1, 10)))
            .thenReturn(expected);

        var result = controller.myClasses(schoolId, "eng", "ACTIVE", 1, 10);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void my_classes_should_reject_invalid_paging() {
        assertThatThrownBy(() -> controller.myClasses(schoolId, null, null, 0, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        assertThatThrownBy(() -> controller.myClasses(schoolId, null, null, 1, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.myClasses(schoolId, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void my_class_should_forward_id_to_use_case() {
        var expected = schoolClassDto();
        when(viewMyClassDetailsUseCase.execute(new ViewMyClassDetailsQuery(classId))).thenReturn(expected);

        assertThat(controller.myClass(classId)).isEqualTo(expected);
    }

    @Test
    void my_class_members_should_forward_filters_to_use_case() {
        var expected = new PageResult<>(List.of(memberDto(UUID.randomUUID())), 1, 20, 1, 1);
        when(viewMyClassMembersUseCase.execute(new ViewMyClassMembersQuery(classId, "STUDENT", "mai", 1, 20)))
            .thenReturn(expected);

        var result = controller.myClassMembers(classId, "STUDENT", "mai", 1, 20);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void my_class_members_should_reject_invalid_paging() {
        assertThatThrownBy(() -> controller.myClassMembers(classId, null, null, 1, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void my_class_school_grade_field_should_reuse_the_shared_loader() {
        var gradeId = UUID.randomUUID();
        var schoolClass = new SchoolClassDto(classId, schoolId, UUID.randomUUID(), gradeId,
            "ENG-01", "English 01", null, "ACTIVE", null, null);
        var expected = new SchoolGradeDto(gradeId, schoolId, UUID.randomUUID(), "G10", "Grade 10", null, null, null, "ACTIVE", null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, SchoolGradeDto>getDataLoader("schoolGradeByClass")).thenReturn(loader);
        when(loader.load(gradeId)).thenReturn(CompletableFuture.completedFuture(expected));

        assertThat(controller.myClassSchoolGrade(schoolClass, env).join()).isEqualTo(expected);
        verify(loader).load(gradeId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void my_class_language_field_should_reuse_the_shared_loader() {
        var languageId = UUID.randomUUID();
        var schoolClass = new SchoolClassDto(classId, schoolId, languageId, UUID.randomUUID(),
            "ENG-01", "English 01", null, "ACTIVE", null, null);
        var expected = new SupportedLanguageDto(languageId, "EN", "English", null, true, null, null);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, SupportedLanguageDto>getDataLoader("supportedLanguageByClass")).thenReturn(loader);
        when(loader.load(languageId)).thenReturn(CompletableFuture.completedFuture(expected));

        assertThat(controller.myClassLanguage(schoolClass, env).join()).isEqualTo(expected);
        verify(loader).load(languageId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void my_class_active_member_count_should_load_by_class_id() {
        var schoolClass = schoolClassDto();
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, Integer>getDataLoader("activeMemberCountByClass")).thenReturn(loader);
        when(loader.load(classId)).thenReturn(CompletableFuture.completedFuture(7));

        assertThat(controller.myClassActiveMemberCount(schoolClass, env).join()).isEqualTo(7);
        verify(loader).load(classId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void my_class_member_user_field_should_load_by_user_id() {
        var userId = UUID.randomUUID();
        var member = memberDto(userId);
        var expected = userDto(userId);
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UUID, UserDto>getDataLoader("userBySchoolClassUser")).thenReturn(loader);
        when(loader.load(userId)).thenReturn(CompletableFuture.completedFuture(expected));

        assertThat(controller.myClassMemberUser(member, env).join()).isEqualTo(expected);
        verify(loader).load(userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void my_class_member_role_codes_should_flatten_roles_to_codes() {
        var userId = UUID.randomUUID();
        var env = mock(DataFetchingEnvironment.class);
        var loader = mock(DataLoader.class);
        when(env.<UserRolesKey, List<RoleDto>>getDataLoader("rolesByUser")).thenReturn(loader);
        when(loader.load(new UserRolesKey(userId))).thenReturn(CompletableFuture.completedFuture(List.of(
            new RoleDto(UUID.randomUUID(), "TEACHER", "Giáo viên", null, null),
            new RoleDto(UUID.randomUUID(), "STUDENT", "Học sinh", null, null)
        )));

        var result = controller.myClassMemberUserRoleCodes(userDto(userId), env).join();

        assertThat(result).containsExactly("TEACHER", "STUDENT");
    }

    private SchoolClassDto schoolClassDto() {
        return new SchoolClassDto(classId, schoolId, UUID.randomUUID(), UUID.randomUUID(),
            "ENG-01", "English 01", "Starter", "ACTIVE", null, null);
    }

    private SchoolClassUserDto memberDto(UUID userId) {
        return new SchoolClassUserDto(UUID.randomUUID(), userId, classId, true, null, null, null);
    }

    private UserDto userDto(UUID userId) {
        return new UserDto(userId, "member@example.com", "0911000001", "Nguyen Van A",
            null, null, null, null, null, null);
    }
}
