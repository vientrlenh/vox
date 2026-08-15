package com.sep.vox.interfaces.rest.mapper;

import java.util.Locale;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CloneSystemRubricToSchoolCommand;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.interfaces.rest.dto.request.CloneSystemRubricRequest;

public class CloneSystemRubricCommandMapper {

    public static CloneSystemRubricToSchoolCommand fromRequest(UUID schoolId, CloneSystemRubricRequest request) {
        return new CloneSystemRubricToSchoolCommand(
                schoolId,
                request.sourceRubricVersionId(),
                request.code(),
                request.name(),
                request.description(),
                toTotalScoreMethod(request.totalScoreMethod())
        );
    }

    /**
     * Trả về null khi client không chọn -- use case hiểu null là "giữ nguyên như bản mẫu".
     *
     * <p>Giá trị lạ đã bị {@code @Pattern} trên request chặn từ trước, nên tới đây chỉ còn hai giá
     * trị hợp lệ hoặc rỗng.
     */
    private static RubricTotalScoreMethod toTotalScoreMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return RubricTotalScoreMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
