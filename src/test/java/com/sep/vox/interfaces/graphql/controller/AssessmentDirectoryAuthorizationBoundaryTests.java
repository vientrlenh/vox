package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * The permission boundary of the "assessment directory" surface.
 *
 * <p>This surface exists so the council chair (a row in `assessment_members`, not a
 * global role) can read exactly what they are already allowed to write. The two tests below
 * lock both ends: the new resolvers must be gated to exactly the two roles that organize an assessment, and the
 * admin surface must not be loosened instead of adding a new surface.
 */
class AssessmentDirectoryAuthorizationBoundaryTests {

    private static final String EXPECTED = "hasAnyRole('SCHOOL_ADMIN', 'TEACHER')";

    @Test
    void every_assessment_directory_resolver_should_be_gated_for_assessment_organisers() {
        List<Method> resolvers = Arrays.stream(AssessmentDirectoryController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(QueryMapping.class)
                || method.isAnnotationPresent(SchemaMapping.class))
            .toList();

        assertThat(resolvers)
            .as("AssessmentDirectoryController phải có đúng 4 query: lớp, niên khóa, học sinh, giám thị")
            .hasSize(4);

        assertThat(resolvers)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s phải gated SCHOOL_ADMIN + TEACHER", method.getName())
                .isEqualTo(EXPECTED));
    }

    @Test
    void admin_directory_surface_should_stay_school_admin_only() {
        Method[] adminOnly = new Method[] {
            declared("schoolClasses"),
            declared("schoolGrades"),
            declared("schoolStudentsBySchool"),
            declared("schoolTeachersBySchool"),
        };

        assertThat(adminOnly)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s không được nới quyền cho TEACHER — thêm query vào AssessmentDirectoryController thay vì nới ở đây",
                    method.getName())
                .isEqualTo("hasRole('SCHOOL_ADMIN')"));
    }

    /**
     * Look up by name instead of a hard signature: these admin queries have many parameters and new
     * filters are added often, but the names are stable. It still asserts that exactly one method
     * is found, so the test does not silently skip when someone renames it.
     */
    private static Method declared(String name) {
        List<Method> candidates = Arrays.stream(SchoolController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(name))
            .toList();
        assertThat(candidates)
            .as("SchoolController.%s phải tồn tại và không bị nạp chồng", name)
            .hasSize(1);
        return candidates.getFirst();
    }

    private static String preAuthorizeValue(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        return annotation == null ? null : annotation.value();
    }
}
