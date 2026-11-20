package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;

import graphql.schema.DataFetchingEnvironment;

/**
 * Ranh giới phân quyền của bề mặt "lớp của tôi".
 *
 * Các resolver dùng lại DataLoader của admin nên chỉ @PreAuthorize mới ngăn được
 * giáo viên đọc dữ liệu ngoài phạm vi lớp mình. Hai bài dưới đây khoá ranh giới
 * đó lại: mọi resolver mới phải gated TEACHER, và bề mặt admin không được nới ra.
 */
class MyClassAuthorizationBoundaryTests {

    @Test
    void every_my_class_resolver_should_require_teacher_role() {
        var resolvers = Arrays.stream(MyClassController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(QueryMapping.class)
                || method.isAnnotationPresent(SchemaMapping.class))
            .toList();

        assertThat(resolvers)
            .as("MyClassController phải có 3 query + 5 field resolver")
            .hasSize(8);

        assertThat(resolvers)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s phải gated TEACHER", method.getName())
                .isEqualTo("hasRole('TEACHER')"));
    }

    @Test
    void admin_class_surface_should_stay_school_admin_only() throws NoSuchMethodException {
        var adminOnly = new Method[] {
            SchoolController.class.getDeclaredMethod("schoolClass", UUID.class),
            SchoolController.class.getDeclaredMethod("schoolClassUsers", UUID.class, Integer.class, Integer.class),
            SchoolController.class.getDeclaredMethod("user", SchoolClassUserDto.class, DataFetchingEnvironment.class),
            SchoolController.class.getDeclaredMethod("schoolGrade", SchoolClassDto.class, DataFetchingEnvironment.class),
            SchoolController.class.getDeclaredMethod("language", SchoolClassDto.class, DataFetchingEnvironment.class),
            SchoolController.class.getDeclaredMethod("school", SchoolClassDto.class, DataFetchingEnvironment.class),
        };

        assertThat(adminOnly)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s không được nới quyền cho TEACHER", method.getName())
                .isEqualTo("hasRole('SCHOOL_ADMIN')"));
    }

    private static String preAuthorizeValue(Method method) {
        var annotation = method.getAnnotation(PreAuthorize.class);
        return annotation == null ? null : annotation.value();
    }
}
