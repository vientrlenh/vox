package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.math.BigDecimal;
import java.util.List;

import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanQuotaCommand;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubscriptionPlanRequest(
    @NotBlank(message = "Tên gói đăng ký không được để trống")
    @Size(max = 255, message = "Tên gói đăng ký không được vượt quá 255 ký tự")
    String name, 
    
    @NotBlank(message = "Mô tả gói không được để trống")
    @Size(max = 2048, message = "Mô tả gói không được vượt quá 2048 ký tự")
    String tagline,

    @NotNull(message = "Giá tiền của gói đăng ký không được để trống")
    BigDecimal priceVnd,

    @NotNull(message = "Kiểu giai đoạn của gói đăng ký không được để trống")
    SubscriptionPlanPeriod periodType,

    @NotNull(message = "Số lượng thời gian giai đoạn gói đăng ký không được để trống")
    @Min(value = 1, message = "Số lượng thời gian giai đoạn gói đăng ký không được nhỏ hơn 1")
    Integer periodCount,

    @NotNull(message = "Số phút của một bài kiểm tra không được để trống")
    @Min(value = 1, message = "Số phút của một bài kiểm tra không được nhỏ hơn 1")
    Integer maxTimePerAttemptMin,

    @NotEmpty(message = "Danh sách các quota cho gói đăng ký không được để trống")
    @Valid
    List<CreateSubscriptionPlanQuotaRequest> quotas
) {

    public static CreateSubscriptionPlanCommand toCommand(CreateSubscriptionPlanRequest request) {
        return new CreateSubscriptionPlanCommand(
            request.name(),
            request.tagline(),
            request.priceVnd(),
            request.periodType(),
            request.periodCount(),
            request.maxTimePerAttemptMin(),
            request.quotas().stream()
                .map(item -> new CreateSubscriptionPlanQuotaCommand(item.quotaType(), item.includedAmountVnd()))
                .toList()
        );
    }
}
