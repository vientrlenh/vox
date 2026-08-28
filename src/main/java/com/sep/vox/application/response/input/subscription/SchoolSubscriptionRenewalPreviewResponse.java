package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;

/**
 * Trường sẽ nhận được gì nếu bấm gia hạn ngay bây giờ -- xem TRƯỚC khi đặt đơn.
 *
 * <p>Màn này tồn tại vì một lý do duy nhất: nếu gói đang dùng đã bị lưu trữ thì gia hạn sẽ TỰ chuyển
 * sang gói thay thế (SubscriptionPlanResolver). Trường phải nhìn thấy và xác nhận điều đó thay vì bị
 * đổi gói mà không hay biết -- id ở {@code renewalPlan} chính là thứ phải gửi lại làm
 * {@code acceptedPlanId} khi đặt đơn (xem RenewSubscriptionUseCase).
 *
 * <p>KHÔNG còn {@code unusedCreditAmount} như bản cũ. Nó là di sản của mô hình cũ, nơi gia hạn sớm
 * ĐÈ lên kỳ đang chạy nên phải bù lại số ngày bị mất. Giờ kỳ mới NỐI TIẾP kỳ cũ, không ngày nào bị
 * mất, nên con số đó vĩnh viễn bằng 0 -- giữ lại một trường luôn bằng 0 chỉ khiến người đọc tưởng
 * hệ thống có tính bù trừ. {@code startsAt} nói đúng điều trường cần biết: tiền trả hôm nay, kỳ mới
 * bắt đầu lúc nào.
 *
 * @param planChanged true khi gói gia hạn khác gói đang dùng -- FE nên làm nổi bật, đây là thứ dễ
 *                    bị bấm qua nhất
 * @param startsAt    "bây giờ" nếu trường không còn kỳ nào hiệu lực; bằng ngày hết hạn của kỳ hiện
 *                    tại nếu gia hạn sớm
 * @param amountDue   giá gói gia hạn, chưa gồm phí dịch vụ -- phí cộng vào lúc đặt đơn
 */
public record SchoolSubscriptionRenewalPreviewResponse(
    boolean planChanged,
    SubscriptionPlanDto currentPlan,
    SubscriptionPlanDto renewalPlan,
    Instant startsAt,
    BigDecimal amountDue
) {

    public static SchoolSubscriptionRenewalPreviewResponse toResponse(
            SubscriptionPlan currentPlan, SubscriptionPlan renewalPlan, Instant startsAt) {
        return new SchoolSubscriptionRenewalPreviewResponse(
            !renewalPlan.getId().equals(currentPlan.getId()),
            SubscriptionPlanDto.toDto(currentPlan),
            SubscriptionPlanDto.toDto(renewalPlan),
            startsAt,
            renewalPlan.getPriceVnd()
        );
    }
}
