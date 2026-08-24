package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

/**
 * @param code           mã rubric do TRƯỜNG đặt, không sao từ bản mẫu. Đây là trục phân biệt giữa
 *                       các bản sao của cùng một bản mẫu (ví dụ ENG-K10 / ENG-K11 / ENG-K12), và
 *                       cũng là thứ ràng buộc unique
 *                       {@code (owner_type, school_id, language_id, framework_id, code)} kiểm tra.
 * @param totalScoreMethod cách tính điểm trường chọn, có thể khác bản mẫu.
 * @param policies       các chính sách mẫu (gắn với chính phiên bản đang sao) mà trường muốn dựng
 *                       luôn cho bản sao. Rỗng = chỉ sao bộ tiêu chí, trường tự tạo chính sách sau.
 */
public record CloneSystemRubricToSchoolCommand(
    UUID schoolId,
    UUID sourceRubricVersionId,
    String code,
    String name,
    String description,
    RubricTotalScoreMethod totalScoreMethod,
    List<PolicyToClone> policies
) {

    /**
     * Một chính sách mẫu, kèm PHẠM VI RIÊNG của nó.
     *
     * <p>Phạm vi phải đi theo từng chính sách chứ không phải theo cả lần sao: nhiều chính sách dùng
     * chung một phiên bản Rubric là để lớp thường và lớp chuyên chấm cùng bộ tiêu chí nhưng khác bậc
     * mục tiêu -- mà mỗi phạm vi chỉ được đúng một chính sách còn hiệu lực
     * ({@code existsActiveForScopeAnyRubricVersion}). Dùng chung một phạm vi cho cả hai thì bản thứ
     * hai bị từ chối ngay trong cùng một lần gọi.
     *
     * <p>Bản mẫu đã khai Khối thì bản sao giữ nguyên khối đó và cả ba cột phạm vi ở đây phải để
     * trống; bản mẫu không khai khối thì phải chọn đúng một trong ba.
     */
    public record PolicyToClone(
        UUID sourcePolicyId,
        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,
        Instant effectiveFrom,
        Instant effectiveTo
    ) {}
}
