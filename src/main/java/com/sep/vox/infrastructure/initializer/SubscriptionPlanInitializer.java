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

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Dựng sẵn một gói đăng ký ACTIVE để trường mới đăng ký được ngay, không phải tạo tay trước.
 *
 * <p>Điều kiện bỏ qua là "đã có gói ACTIVE nào chưa", không phải "bảng có rỗng không": gói ở
 * {@code DRAFT} thì trường KHÔNG đăng ký được, nên một cơ sở dữ liệu chỉ có bản nháp vẫn cần gói
 * này. Ngược lại, đã có gói ACTIVE rồi thì thêm nữa chỉ làm rối màn chọn gói.
 *
 * <p><b>Cập nhật theo V2 (orders/payments/school balance).</b> Hạn mức không còn đo bằng token hay
 * bằng USD chi phí AI nữa mà bằng TIỀN: {@code plan_quota.included_quantity} đã đổi thành
 * {@code subscription_plan_quotas.included_amount_vnd}, và {@code token_unit_price} bị bỏ hẳn. Vì
 * vậy lớp này không còn cần {@code QuotaPricingPort} -- không còn con số đơn giá nào để tra.
 */
@Component
@Order(10)
public class SubscriptionPlanInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionPlanInitializer.class);

    private static final String PLAN_NAME = "Gói Tiêu chuẩn";
    private static final String PLAN_TAGLINE = "Gói mặc định dựng sẵn cho trường mới";

    /**
     * Hạn mức mỗi loại, đơn vị VNĐ.
     *
     * <p>Giữ nguyên ý định của bản trước -- "cấp dư để không chặn giữa buổi trình bày" -- chỉ đổi
     * đơn vị theo V2. Bản cũ cấp 1000 USD chi phí AI mỗi loại; quy theo đúng tỷ giá đã ghi trong
     * chú thích cũ (31.263₫/USD) là ~31,3 triệu, nên lấy tròn 31.263.000₫ để con số vẫn truy được
     * về gốc thay vì bịa một số mới.
     */
    private static final BigDecimal INCLUDED_AMOUNT_VND = new BigDecimal("31263000");

    /**
     * Giá bán CỐ ĐỊNH, mang tính tượng trưng cho demo -- KHÔNG suy ra từ hạn mức.
     *
     * <p>Bản đầu tính giá = tổng chi phí AI cấp × đơn giá, ra 93.789.000₫. Đúng về mặt kinh doanh
     * nhưng vô dụng cho demo: không ai bấm mua một gói 93 triệu để xem thử luồng đăng ký.
     *
     * <p>Hệ quả phải nói rõ: gói này bán LỖ nặng so với hạn mức nó cấp. Đây là lựa chọn có chủ đích
     * cho môi trường demo, KHÔNG phải công thức giá dùng được thật. Gói tạo từ giao diện vẫn tính
     * giá theo cách của nó, không dính gì tới hằng số này.
     */
    private static final BigDecimal PRICE_VND = new BigDecimal("10000");

    /** Một năm: V2 thay {@code validity_days} bằng cặp (loại chu kỳ, số chu kỳ). */
    private static final SubscriptionPlanPeriod PERIOD_TYPE = SubscriptionPlanPeriod.YEAR;
    private static final int PERIOD_COUNT = 1;

    /**
     * Trần độ dài MỘT lượt thi. Đây là cái thước, không phải túi tiền -- nó không cạn đi khi thi
     * nhiều, chỉ chặn mã đề dài quá mức. Để 60 phút cho rộng hơn hẳn mã đề demo (khoảng 20-25 phút,
     * đã gồm thời lượng audio/video) để không chặn nhầm lúc soạn đề.
     */
    private static final int MAX_TIME_PER_ATTEMPT_MIN = 60;

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    public SubscriptionPlanInitializer(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!subscriptionPlanRepository.findByStatus(SubscriptionPlanStatus.ACTIVE).isEmpty()) {
            LOGGER.info("Active subscription plan already exists. Skip initialize");
            return;
        }

        // V2 gộp GRADING và CLASS_TEST vào EXAM, nên chỉ còn hai loại hạn mức.
        var quotaTypes = List.of(QuotaType.EXAM, QuotaType.PRACTICE);

        var now = Instant.now();
        var plan = subscriptionPlanRepository.save(new SubscriptionPlan(
            PLAN_NAME,
            PLAN_TAGLINE,
            PRICE_VND,
            PERIOD_TYPE,
            PERIOD_COUNT,
            MAX_TIME_PER_ATTEMPT_MIN,
            // ACTIVE ngay, không phải DRAFT: mục đích của gói này là dùng được luôn, mà DRAFT thì
            // trường không đăng ký được.
            SubscriptionPlanStatus.ACTIVE,
            1L,
            now,
            now,
            // Không có người dùng đăng nhập lúc khởi động; cột created_by/updated_by cho phép null.
            null,
            null
        ));

        for (var quotaType : quotaTypes) {
            subscriptionPlanQuotaRepository.save(new SubscriptionPlanQuota(
                plan.getId(),
                quotaType,
                INCLUDED_AMOUNT_VND
            ));
        }

        LOGGER.info(
            "Subscription plan initialized successfully: {} ({}đ/nam, {}đ moi loai han muc, {} loai)",
            PLAN_NAME,
            PRICE_VND.toPlainString(),
            INCLUDED_AMOUNT_VND.toPlainString(),
            quotaTypes.size()
        );
    }
}
