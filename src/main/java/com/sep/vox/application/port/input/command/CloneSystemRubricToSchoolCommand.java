package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

/**
 * @param code           mã rubric do TRƯỜNG đặt, không sao từ bản mẫu. Đây là trục phân biệt giữa
 *                       các bản sao của cùng một bản mẫu (ví dụ ENG-K10 / ENG-K11 / ENG-K12), và
 *                       cũng là thứ ràng buộc unique
 *                       {@code (owner_type, school_id, language_id, framework_id, code)} kiểm tra.
 * @param totalScoreMethod cách tính điểm trường chọn, có thể khác bản mẫu.
 */
public record CloneSystemRubricToSchoolCommand(
    UUID schoolId,
    UUID sourceRubricVersionId,
    String code,
    String name,
    String description,
    RubricTotalScoreMethod totalScoreMethod
) {
}
