package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.UpdateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.UpdateSubscriptionPlanQuotaCommand;

/**
 * Khớp với input UpdateSubscriptionPlanInput trong subscription.graphqls: mọi field đều nullable với
 * nghĩa "không đụng tới". Không có periodType (cột period_type là updatable = false) và không có
 * serviceFeeRatio (đã thành config toàn hệ thống) -- xem {@link
 * com.sep.vox.application.port.input.command.UpdateSubscriptionPlanCommand}.
 */
public record UpdateSubscriptionPlanInput(
    String name,
    String tagline,
    BigDecimal priceVnd,
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    List<SubscriptionPlanQuotaInput> quotas
) {

    public static UpdateSubscriptionPlanCommand toCommand(UUID id, UpdateSubscriptionPlanInput input) {
        return new UpdateSubscriptionPlanCommand(
            id,
            input.name(),
            input.tagline(),
            input.priceVnd(),
            input.periodCount(),
            input.maxTimePerAttemptMin(),
            // null (không sửa hạn mức) khác hẳn danh sách rỗng (xóa sạch hạn mức) nên phải giữ
            // nguyên null xuống tới use case thay vì quy về List.of().
            //
            // quotaType đổi về String vì command mang chuỗi thô giống CreateSubscriptionPlanCommand:
            // use case tự parse và tự báo lỗi để còn dùng được từ lối vào khác (REST, job) mà không
            // dựa vào việc graphql-java đã kiểm hộ.
            input.quotas() == null
                ? null
                : input.quotas().stream()
                    .map(item -> new UpdateSubscriptionPlanQuotaCommand(
                        item.quotaType() == null ? null : item.quotaType().name(),
                        item.includedAmountVnd()
                    ))
                    .toList()
        );
    }
}
