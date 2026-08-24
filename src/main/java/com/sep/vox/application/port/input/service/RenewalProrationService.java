package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.subscription.SchoolSubscription;

/**
 * Tính khoản bù cho số ngày CHƯA DÙNG của gói cũ khi trường bị ép đổi sang gói thay thế lúc gia
 * hạn (gói đang dùng đã bị System Admin archive giữa chu kỳ -- xem SubscriptionPlanResolver). Chỉ
 * áp dụng khi thật sự đổi gói (planChanged); gia hạn bình thường không đổi gói thì không bù gì.
 *
 * <p>Bù theo TỈ LỆ NGÀY CÒN LẠI (không theo hạn mức AI chưa dùng -- quota USD là trần chi phí AI
 * worst-case VOX tự đặt ra để phòng thân, không phải đơn vị đo giá trị subscription, dùng nó để bù
 * tiền sẽ gần như luôn trả lại gần hết giá gói vì trần luôn set rộng hơn mức dùng thật rất nhiều).
 * Trường trả tiền để mua quyền truy cập theo THỜI GIAN, nên bù theo ngày còn lại mới đúng bản chất
 * subscription, giống mô hình proration chuẩn của các SaaS khác (Stripe...).
 *
 * <p>Dùng chung ở cả PreviewRenewalUseCase (hiện trước cho trường xem) lẫn
 * CreatePaymentLinkForRenewalUseCase (thu tiền thật) để 2 nơi LUÔN ra cùng 1 con số.
 */
@Service
public class RenewalProrationService {

    public BigDecimal calculateUnusedCredit(
            SchoolSubscription current, BigDecimal renewalPlanPrice, boolean planChanged, LocalDate today) {
        if (!planChanged) {
            return BigDecimal.ZERO;
        }

        var totalDays = ChronoUnit.DAYS.between(current.getStartDate(), current.getEndDate());
        var remainingDays = ChronoUnit.DAYS.between(today, current.getEndDate());
        if (totalDays <= 0 || remainingDays <= 0) {
            return BigDecimal.ZERO;
        }

        var credit = current.getPricePaidSnapshot()
            .multiply(BigDecimal.valueOf(remainingDays))
            .divide(BigDecimal.valueOf(totalDays), 0, RoundingMode.HALF_UP);
        // Không cho amountDue (renewalPlanPrice - credit) âm.
        return credit.min(renewalPlanPrice);
    }
}
