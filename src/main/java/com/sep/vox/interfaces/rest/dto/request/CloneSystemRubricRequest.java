package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CloneSystemRubricRequest(
        @NotNull(message = "Phiên bản bộ tiêu chí mẫu không được để trống")
        UUID sourceRubricVersionId,

        // Mã do TRƯỜNG đặt, không sao từ bản mẫu: đây là thứ phân biệt các bản sao của cùng một bản
        // mẫu (ENG-K10 / ENG-K11 / ENG-K12) và cũng là thứ ràng buộc unique của bảng rubrics kiểm.
        @NotBlank(message = "Mã bộ tiêu chí không được để trống")
        @Size(max = 50, message = "Mã bộ tiêu chí tối đa 50 ký tự")
        String code,

        @NotBlank(message = "Tên bộ tiêu chí không được để trống")
        @Size(max = 255, message = "Tên bộ tiêu chí tối đa 255 ký tự")
        String name,

        @Size(max = 2048, message = "Mô tả tối đa 2048 ký tự")
        String description,

        /**
         * Cách tính điểm trường chọn cho bản sao; bỏ trống thì giữ nguyên như bản mẫu.
         *
         * <p>Chọn WEIGHTED_AVERAGE nghĩa là bỏ tỉ lệ phân bổ của bản mẫu và cho mọi tiêu chí cân
         * bằng -- đó là ý nghĩa của cách tính đó, không phải mất mát ngoài ý muốn. Chiều ngược lại
         * không suy ra được, nên bản mẫu nên soạn ở dạng SUM.
         */
        @Pattern(regexp = "SUM|WEIGHTED_AVERAGE", message = "Cách tính điểm chỉ nhận SUM hoặc WEIGHTED_AVERAGE")
        String totalScoreMethod,

        /**
         * Các chính sách chấm mẫu (gắn với chính phiên bản đang sao) mà trường muốn dựng luôn cho
         * bản sao. Bỏ trống = chỉ sao bộ tiêu chí; khi đó phiên bản mới nằm DRAFT tới khi trường tự
         * gắn một chính sách, vì ban hành phiên bản đòi phải có chính sách liên kết đã PUBLISHED.
         */
        @Valid
        List<ClonePolicyChoice> policies
) {

    /**
     * Phạm vi đi theo TỪNG chính sách, không phải theo cả lần sao: mỗi phạm vi chỉ được đúng một
     * chính sách còn hiệu lực, nên sao hai bản mẫu (ví dụ Bậc 3 cho lớp thường, Bậc 4 cho lớp
     * chuyên) vào cùng một phạm vi sẽ bị từ chối ngay trong cùng một lần gọi.
     */
    public record ClonePolicyChoice(
            @NotNull(message = "Chính sách mẫu không được để trống")
            UUID sourcePolicyId,

            // Chỉ điền khi bản mẫu KHÔNG gắn Khối; khi đó phải chọn đúng 1 trong 3. Bản mẫu đã gắn
            // Khối thì bản sao giữ nguyên khối đó và cả ba phải để trống.
            UUID gradeLevelId,
            UUID schoolGradeId,
            UUID schoolClassId,

            @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
            String effectiveFrom,

            String effectiveTo
    ) {}
}
