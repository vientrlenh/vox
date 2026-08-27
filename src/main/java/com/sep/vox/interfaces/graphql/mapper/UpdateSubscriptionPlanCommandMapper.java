package com.sep.vox.interfaces.graphql.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.UpdateSubscriptionPlanQuotaCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSubscriptionPlanInput;

public final class UpdateSubscriptionPlanCommandMapper {

    private UpdateSubscriptionPlanCommandMapper() {
    }

    public static UpdateSubscriptionPlanCommand fromInput(UUID id, UpdateSubscriptionPlanInput input) {
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
