package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;

import graphql.schema.DataFetchingEnvironment;

/**
 * Authorization boundary of the "my classes" surface.
 *
 * The resolvers reuse the admin DataLoaders, so only @PreAuthorize can stop a
 * teacher from reading data outside their own classes. The two tests below lock that boundary:
 * every new resolver must be gated to TEACHER, and the admin surface must not be widened.
 */
class MyClassAuthorizationBoundaryTests {

    @Test
    void every_my_class_resolver_should_require_teacher_role() {
        List<Method> resolvers = Arrays.stream(GraphQLMyClassController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(QueryMapping.class)
                || method.isAnnotationPresent(SchemaMapping.class))
            .toList();

        assertThat(resolvers)
            .as("GraphQLMyClassController must have 3 queries + 5 field resolvers")
            .hasSize(8);

        assertThat(resolvers)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s must be gated to TEACHER", method.getName())
                .isEqualTo("hasRole('TEACHER')"));
    }

    @Test
    void admin_class_surface_should_stay_school_admin_only() throws NoSuchMethodException {
        Method[] adminOnly = new Method[] {
            GraphQLSchoolController.class.getDeclaredMethod("schoolClass", UUID.class),
            GraphQLSchoolController.class.getDeclaredMethod("schoolClassUsers", UUID.class, Integer.class, Integer.class),
            GraphQLSchoolController.class.getDeclaredMethod("user", SchoolClassUserDto.class, DataFetchingEnvironment.class),
            GraphQLSchoolController.class.getDeclaredMethod("schoolGrade", SchoolClassDto.class, DataFetchingEnvironment.class),
            GraphQLSchoolController.class.getDeclaredMethod("language", SchoolClassDto.class, DataFetchingEnvironment.class),
            GraphQLSchoolController.class.getDeclaredMethod("school", SchoolClassDto.class, DataFetchingEnvironment.class),
        };

        assertThat(adminOnly)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s must not widen permission to TEACHER", method.getName())
                .isEqualTo("hasRole('SCHOOL_ADMIN')"));
    }

    private static String preAuthorizeValue(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        return annotation == null ? null : annotation.value();
    }
}
