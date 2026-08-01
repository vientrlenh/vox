package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Ranh giới phân quyền của bề mặt "danh bạ kỳ thi".
 *
 * <p>Bề mặt này tồn tại để chủ tịch hội đồng (một dòng trong `exam_members`, không phải
 * vai trò toàn cục) đọc được đúng những gì họ vốn đã được phép ghi. Hai bài dưới đây
 * khoá hai đầu lại: resolver mới phải gated đúng hai vai trò tổ chức kỳ thi, và bề mặt
 * admin không được nới ra thay vì thêm bề mặt mới.
 */
class ExamDirectoryAuthorizationBoundaryTests {

    private static final String EXPECTED = "hasAnyRole('SCHOOL_ADMIN', 'TEACHER')";

    @Test
    void every_exam_directory_resolver_should_be_gated_for_exam_organisers() {
        var resolvers = Arrays.stream(ExamDirectoryController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(QueryMapping.class)
                || method.isAnnotationPresent(SchemaMapping.class))
            .toList();

        assertThat(resolvers)
            .as("ExamDirectoryController phải có đúng 4 query: lớp, niên khóa, học sinh, giám thị")
            .hasSize(4);

        assertThat(resolvers)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s phải gated SCHOOL_ADMIN + TEACHER", method.getName())
                .isEqualTo(EXPECTED));
    }

    @Test
    void admin_directory_surface_should_stay_school_admin_only() {
        var adminOnly = new Method[] {
            declared("schoolClasses"),
            declared("schoolGrades"),
            declared("schoolStudentsBySchool"),
            declared("schoolTeachersBySchool"),
        };

        assertThat(adminOnly)
            .allSatisfy(method -> assertThat(preAuthorizeValue(method))
                .as("%s không được nới quyền cho TEACHER — thêm query vào ExamDirectoryController thay vì nới ở đây",
                    method.getName())
                .isEqualTo("hasRole('SCHOOL_ADMIN')"));
    }

    /**
     * Tra theo tên thay vì chữ ký cứng: các query admin này có nhiều tham số và hay được
     * thêm bộ lọc mới, nhưng tên thì ổn định. Vẫn khẳng định tìm thấy đúng một method để
     * bài test không âm thầm bỏ qua khi ai đó đổi tên.
     */
    private static Method declared(String name) {
        var candidates = Arrays.stream(SchoolController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(name))
            .toList();
        assertThat(candidates)
            .as("SchoolController.%s phải tồn tại và không bị nạp chồng", name)
            .hasSize(1);
        return candidates.getFirst();
    }

    private static String preAuthorizeValue(Method method) {
        var annotation = method.getAnnotation(PreAuthorize.class);
        return annotation == null ? null : annotation.value();
    }
}
