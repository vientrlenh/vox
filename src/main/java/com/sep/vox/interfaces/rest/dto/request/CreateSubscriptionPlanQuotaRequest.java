package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateSubscriptionPlanQuotaRequest(
    @NotBlank(message = "Loại quota không được để trống")
    // Phải khớp enum QuotaType. Regex cũ còn liệt kê GRADING/CLASS_TEST -- hai giá trị đã bị bỏ (xem
    // QuotaType, V2 mục 11) -- nên nó vừa TỪ CHỐI giá trị hợp lệ duy nhất là EXAM, vừa nhận hai giá
    // trị mà QuotaType.valueOf() phía sau chắc chắn ném IllegalArgumentException: cấu hình hạn mức
    // cho một gói mới không có đường nào đi lọt.
    @Pattern(regexp = "^(EXAM|PRACTICE)$", message = "Chỉ chấp nhận các loại quota EXAM/PRACTICE")
    String quotaType,
    // Cột subscription_plan_quotas.included_amount_vnd là numeric(18,6) -- chỉ chứa được giá trị tuyệt đối < 10^12,
    // vượt quá sẽ tràn số ở DB (numeric field overflow) thay vì báo lỗi rõ ràng ở đây.
    @NotNull(message = "Số tiền của một loại quota không được để trống")
    @DecimalMax(value = "999999999999.999999", message = "Số tiền của một loại quota không được vượt quá 999999999999.999999") 
    BigDecimal includedAmountVnd
) {
}
