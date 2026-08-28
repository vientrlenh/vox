package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * KHÔNG có schoolId lẫn subscriptionId: cả hai suy ra từ token, nên không có đường gia hạn hộ trường
 * khác. Xem RenewSchoolSubscriptionUseCase.
 *
 * @param acceptedPlanId gói mà trường ĐÃ NHÌN THẤY và đồng ý ở màn xem trước. Bắt buộc, kể cả khi
 *                       gói không đổi: đây là bằng chứng trường biết mình đang mua gì, và là thứ
 *                       chặn được ca gói bị đổi thay thế giữa lúc trường còn đang xem màn trước đó.
 */
public record RenewSchoolSubscriptionCommand(
    UUID acceptedPlanId
) {
}
