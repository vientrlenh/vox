package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
        String totalScoreMethod
) {}
