package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingCriterionMetaInfo;
import com.sep.vox.application.query.dto.GradingCriterionScoreInfo;
import com.sep.vox.application.query.dto.GradingExamOptionInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.GradingTaskItemInfo;
import com.sep.vox.application.query.dto.GradingTurnInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;

/**
 * Spring GraphQL nối field với property theo TÊN. Lệch một chữ thì field đó trả
 * về {@code null} — không lỗi, không log, FE chỉ thấy ô trống. Test này bắt lệch
 * ngay tại chỗ thay vì để lộ ra ở màn hình.
 */
class GradingSchemaBindingTests {

    private static final Path SCHEMA =
        Path.of("src", "main", "resources", "graphql", "exam-grading.graphqls");

    /** Neo vào đầu dòng để {@code extend type Query} không bị nhận là một object type. */
    private static final Pattern TYPE_BLOCK = Pattern.compile(
        "^type\\s+(\\w+)\\s*\\{([^}]*)}", Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern FIELD = Pattern.compile(
        "^\\s*(\\w+)\\s*(?:\\([^)]*\\))?\\s*:", Pattern.MULTILINE);

    static Stream<Arguments> typesBackedByDto() {
        return Stream.of(
            Arguments.of("GradingStats", GradingStatsInfo.class),
            Arguments.of("GradingTeacherProgress", GradingStatsInfo.TeacherProgress.class),
            Arguments.of("GradingResultStatusCount", GradingStatsInfo.ResultStatusCount.class),
            Arguments.of("GradingTurn", GradingTurnInfo.class),
            Arguments.of("GradingCriterionScore", GradingCriterionScoreInfo.class),
            Arguments.of("GradingCriterionMeta", GradingCriterionMetaInfo.class),
            Arguments.of("GradingTaskItem", GradingTaskItemInfo.class),
            Arguments.of("GradingTaskDetail", GradingTaskDetailInfo.class),
            Arguments.of("GradingTask", GradingTaskInfo.class),
            Arguments.of("GradingAssignmentRow", GradingAssignmentRowInfo.class),
            Arguments.of("AssignableTeacher", AssignableTeacherInfo.class),
            Arguments.of("GradingExamOption", GradingExamOptionInfo.class),
            Arguments.of("ResultStatusHistoryEntry", ResultStatusHistoryInfo.class),
            Arguments.of("AiQualityReport", AiQualityReportInfo.class),
            Arguments.of("AiQualityByTeacher", AiQualityReportInfo.ByTeacher.class)
        );
    }

    /**
     * Field lấy dữ liệu qua {@code @SchemaMapping} + DataLoader chứ không phải property của DTO,
     * nên cố tình KHÔNG có record component cùng tên -- xem GradingController.
     *
     * <p>Liệt kê tường minh thay vì nới lỏng phép kiểm: mọi field còn lại vẫn phải khớp tên, vì
     * lệch tên là trả {@code null} âm thầm, đúng thứ test này sinh ra để bắt. Thêm resolver mới thì
     * thêm một dòng ở đây, và việc phải sửa test là chủ ý -- nó buộc người thêm phải nhìn lại.
     */
    private static final Set<String> RESOLVER_BACKED_FIELDS = Set.of(
        "AssignableTeacher.user",
        "GradingTeacherProgress.teacher",
        "AiQualityByTeacher.teacher"
    );

    @ParameterizedTest(name = "{0}")
    @MethodSource("typesBackedByDto")
    void should_back_every_schema_field_with_a_dto_component(String typeName, Class<?> dto) {
        var componentNames = Stream.of(dto.getRecordComponents())
            .map(component -> component.getName())
            .toList();

        var propertyBoundFields = fieldsOf(typeName).stream()
            .filter(field -> !RESOLVER_BACKED_FIELDS.contains(typeName + "." + field))
            .toList();

        assertThat(propertyBoundFields)
            .describedAs("field của %s phải có component cùng tên trên %s", typeName, dto.getSimpleName())
            .isNotEmpty()
            .allSatisfy(field -> assertThat(componentNames).contains(field));
    }

    @Test
    void should_cover_every_object_type_declared_in_the_schema() {
        // Trừ hai type phân trang: chúng map sang PageResult dùng chung, không có DTO riêng.
        var declared = new ArrayList<String>();
        var matcher = TYPE_BLOCK.matcher(read());
        while (matcher.find()) {
            declared.add(matcher.group(1));
        }
        declared.removeAll(List.of("GradingTaskPage", "GradingAssignmentRowPage"));

        var covered = typesBackedByDto().map(arguments -> (String) arguments.get()[0]).toList();
        assertThat(declared).containsExactlyInAnyOrderElementsOf(covered);
    }

    private List<String> fieldsOf(String typeName) {
        var matcher = TYPE_BLOCK.matcher(read());
        while (matcher.find()) {
            if (matcher.group(1).equals(typeName)) {
                return FIELD.matcher(stripComments(matcher.group(2))).results()
                    .map(result -> result.group(1))
                    .toList();
            }
        }
        throw new AssertionError("Không tìm thấy type " + typeName + " trong " + SCHEMA);
    }

    /** Docstring có dấu ':' nên phải bỏ trước khi dò field, không thì nhận nhầm. */
    private String stripComments(String body) {
        return body.replaceAll("(?s)\"\"\".*?\"\"\"", "")
            .replaceAll("(?m)^\\s*\"[^\"]*\"\\s*$", "")
            .replaceAll("(?m)#.*$", "");
    }

    private String read() {
        try {
            return Files.readString(SCHEMA, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
