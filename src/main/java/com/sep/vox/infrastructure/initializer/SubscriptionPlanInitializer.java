package com.sep.vox.infrastructure.initializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Dựng sẵn một gói đăng ký ACTIVE để trường mới đăng ký được ngay, không phải tạo tay trước.
 *
 * <p>Điều kiện bỏ qua là "đã có gói ACTIVE nào chưa", không phải "bảng có rỗng không": gói ở
 * {@code DRAFT} thì trường KHÔNG đăng ký được, nên một cơ sở dữ liệu chỉ có bản nháp vẫn cần gói
 * này. Ngược lại, đã có gói ACTIVE rồi thì thêm nữa chỉ làm rối màn chọn gói.
 *
 * <p>Giá và {@code tokenUnitPrice} KHÔNG gán số cứng mà lấy qua {@link QuotaPricingPort}, đúng cách
 * {@code CreatePlanUseCase} làm -- gán cứng thì gói seed sẽ lệch giá so với mọi gói tạo từ giao
 * diện ngay khi tỷ giá đổi, và lệch kiểu im lặng.
 */
@Component
@Order(10)
public class SubscriptionPlanInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionPlanInitializer.class);

    private static final String PLAN_NAME = "Gói Tiêu chuẩn";
    private static final String PLAN_TAGLINE = "Gói mặc định dựng sẵn cho trường mới";

    /** Khớp DEFAULT_SERVICE_FEE_RATIO của CreatePlanUseCase. */
    private static final BigDecimal SERVICE_FEE_RATIO = new BigDecimal("0.20");

    /**
     * Hạn mức mỗi loại, đơn vị USD chi phí AI (xem {@code plan_quota.included_quantity}).
     * Rộng tay có chủ đích: đây là gói để chạy demo/thử nghiệm, chặn hạn mức giữa buổi trình bày
     * gây khó chịu hơn nhiều so với việc cấp dư.
     */
    private static final BigDecimal INCLUDED_QUANTITY_USD = new BigDecimal("1000");

    private static final int VALIDITY_DAYS = 365;

    /**
     * Trần độ dài MỘT lượt thi. Đây là cái thước, không phải túi tiền -- nó không cạn đi khi thi
     * nhiều, chỉ chặn mã đề dài quá mức. Để 60 phút cho rộng hơn hẳn mã đề demo (khoảng 20-25 phút,
     * đã gồm thời lượng audio/video) để không chặn nhầm lúc soạn đề.
     */
    private static final int MAX_TIME_PER_ATTEMPT_MIN = 60;

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final QuotaPricingPort quotaPricingPort;

    public SubscriptionPlanInitializer(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            QuotaPricingPort quotaPricingPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.quotaPricingPort = quotaPricingPort;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!subscriptionPlanRepository.findAllByStatus(PlanStatus.ACTIVE).isEmpty()) {
            LOGGER.info("Active subscription plan already exists. Skip initialize");
            return;
        }

        var tokenUnitPrice = quotaPricingPort.tokenUnitPriceFor(SERVICE_FEE_RATIO);
        var quotaTypes = List.of(QuotaType.GRADING, QuotaType.CLASS_TEST, QuotaType.PRACTICE);

        // Giá năm suy TỪ hạn mức thay vì là một con số rời: tổng USD cấp × giá bán mỗi USD. Viết
        // một số tuỳ ý vào đây sẽ tạo ra một gói bán lỗ hoặc lãi vô lý mà không ai nhận ra.
        var pricePerYear = INCLUDED_QUANTITY_USD
            .multiply(BigDecimal.valueOf(quotaTypes.size()))
            .multiply(tokenUnitPrice);

        var plan = subscriptionPlanRepository.save(new SubscriptionPlan(
            PLAN_NAME,
            PLAN_TAGLINE,
            pricePerYear,
            VALIDITY_DAYS,
            MAX_TIME_PER_ATTEMPT_MIN,
            // ACTIVE ngay, không phải DRAFT: mục đích của gói này là dùng được luôn, mà DRAFT thì
            // trường không đăng ký được.
            PlanStatus.ACTIVE,
            1,
            Instant.now(),
            // Không có người dùng đăng nhập lúc khởi động; cột created_by cho phép null.
            null,
            SERVICE_FEE_RATIO
        ));

        for (var quotaType : quotaTypes) {
            planQuotaRepository.save(new PlanQuota(
                plan.getId(),
                quotaType,
                INCLUDED_QUANTITY_USD,
                tokenUnitPrice
            ));
        }

        LOGGER.info(
            "Subscription plan initialized successfully: {} ({} VND/nam, {} USD moi loai han muc)",
            PLAN_NAME,
            pricePerYear.toPlainString(),
            INCLUDED_QUANTITY_USD.toPlainString()
        );
    }
}
