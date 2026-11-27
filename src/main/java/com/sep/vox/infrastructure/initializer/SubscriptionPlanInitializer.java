package com.sep.vox.infrastructure.initializer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Seeds a LADDER of ACTIVE subscription plans so that a new school can register immediately, without anyone creating
 * them by hand first.
 *
 * <p>The six plans cover all three period types: one 7-day trial plan, three monthly plans (1/3/6) and two yearly plans
 * (1/2). A single plan, as in the earlier version, is enough to register but NOT enough to exercise the plan-change flow:
 * the decision "take effect immediately and credit the unused part" or "queue after the current period" rests on a PRICE
 * comparison between the new plan and the running plan, and one plan gives nothing to compare.
 *
 * <p><b>Skipping is per PLAN, not per run.</b> The earlier version skipped everything if any ACTIVE plan already existed,
 * which meant a running database would never receive plans added to this list later. Now each plan is matched by NAME, so
 * adding a plan to {@link #PLANS} makes it appear at the next startup without touching the plans that already exist.
 *
 * <p>The match is over EVERY status, not only ACTIVE: an administrator archiving (ARCHIVED) a plan is a decision, and
 * re-creating it at the next startup would overwrite that decision.
 *
 * <p><b>The quota is measured in MONEY.</b> Since V2, {@code plan_quota.included_quantity} became
 * {@code subscription_plan_quotas.included_amount_vnd} and {@code token_unit_price} was dropped entirely -- so this class
 * no longer needs {@code QuotaPricingPort}, there is no unit price left to look up.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanInitializer implements ApplicationRunner {

    /**
     * FLAT conversion rate, nothing added on top.
     *
     * <p>Equal to {@code DEFAULT_USD_TO_VND_RATE} of {@code QuotaSellingPriceProperties}: the market
     * rate, NOT multiplied by {@code (1 + serviceFeeRatio)}. Baking the margin into the rate is exactly
     * what V2 got rid of -- it produces a "unit price" that a school cannot check against any external
     * rate. The service fee may only appear as a separate fee line on the order, decided by
     * {@code ServiceFeePort} when the order is placed.
     */
    private static final BigDecimal USD_TO_VND = new BigDecimal("26000");

    /** V2 merged GRADING and CLASS_TEST into ASSESSMENT, so only two quota types remain. */
    private static final List<QuotaType> QUOTA_TYPES = List.of(QuotaType.ASSESSMENT, QuotaType.PRACTICE);

    /**
     * Length cap of ONE attempt, shared by the whole plan ladder. It is a ruler, not a wallet --
     * it does not run out when many attempts are taken, it only blocks paper versions that are too long. 60 minutes is
     * well above the demo paper (about 20-25 minutes, audio/video duration included) so a paper is not blocked by mistake
     * while it is being authored.
     */
    private static final int MAX_TIME_PER_ATTEMPT_MIN = 60;

    /** Large enough to load the whole plan catalogue in one read -- the catalogue is counted in tens, not thousands. */
    private static final int LOOKUP_PAGE_SIZE = 500;

    /**
     * THE ORDER OF THIS LIST IS DELIBERATE: from MOST EXPENSIVE down to CHEAPEST.
     *
     * <p>The plan list returned to a school is sorted by {@code id DESC} (see
     * {@code SubscriptionPlanRepositoryImpl.NEWEST_FIRST}), and the id is a uuidv7 that grows in
     * insertion order. Inserting from expensive to cheap therefore gives a plan picker that reads from cheap to
     * expensive -- the way a price table is read.
     *
     * <p>This is a SOFT constraint, not a guarantee: two plans inserted in the same millisecond may swap
     * places. To be certain, the read path would have to sort by price, and that is a separate behavior change.
     *
     * <p>The prices here are the DEMO ladder (10k → 280k), deliberately far cheaper than the AI cost the plan grants -- see
     * {@link PlanSeed}. The quotas are still real numbers, converted from a USD budget at the flat rate.
     */
    private static final List<PlanSeed> PLANS = List.of(
        new PlanSeed(
            "Gói 2 năm",
            "Tiết kiệm nhất — dành cho trường đã dùng ổn định",
            SubscriptionPlanPeriod.YEAR, 2,
            new BigDecimal("2000"),
            new BigDecimal("280000")
        ),
        new PlanSeed(
            "Gói 1 năm",
            "Trọn một năm học, hạn mức cấp một lần",
            SubscriptionPlanPeriod.YEAR, 1,
            new BigDecimal("1000"),
            new BigDecimal("160000")
        ),
        new PlanSeed(
            "Gói 6 tháng",
            "Trọn một học kỳ",
            SubscriptionPlanPeriod.MONTH, 6,
            new BigDecimal("500"),
            new BigDecimal("90000")
        ),
        new PlanSeed(
            "Gói 3 tháng",
            "Đủ cho một đợt ôn và thi giữa kỳ",
            SubscriptionPlanPeriod.MONTH, 3,
            new BigDecimal("250"),
            new BigDecimal("50000")
        ),
        new PlanSeed(
            "Gói 1 tháng",
            "Trả theo tháng, dừng lúc nào cũng được",
            SubscriptionPlanPeriod.MONTH, 1,
            new BigDecimal("85"),
            new BigDecimal("20000")
        ),
        new PlanSeed(
            "Gói dùng thử 7 ngày",
            "Chạy thử toàn bộ quy trình trước khi quyết định",
            SubscriptionPlanPeriod.DAY, 7,
            new BigDecimal("20"),
            new BigDecimal("10000")
        )
    );

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Set<String> existingNames = subscriptionPlanRepository.findAll(1, LOOKUP_PAGE_SIZE).content().stream()
            .map(plan -> plan.getName())
            .collect(Collectors.toSet());

        int created = 0;
        for (PlanSeed seed : PLANS) {
            if (existingNames.contains(seed.name())) {
                continue;
            }
            createPlan(seed);
            created++;
        }

        if (created == 0) {
            log.info("Subscription plan catalogue already complete ({} plans). Skip initialize", PLANS.size());
            return;
        }

        log.info("Subscription plan catalogue initialized: {} of {} plans created", created, PLANS.size());
    }

    private void createPlan(PlanSeed seed) {
        BigDecimal includedAmountVnd = seed.includedAmountVnd();

        SubscriptionPlan plan = saveActivePlan(seed);
        saveEqualQuotas(plan.getId(), includedAmountVnd);

        log.info(
            "Subscription plan created: {} ({} {}, gia {}d, han muc {}d moi loai)",
            seed.name(),
            seed.periodCount(),
            seed.periodType(),
            seed.priceVnd().toPlainString(),
            includedAmountVnd.toPlainString()
        );
    }

    private SubscriptionPlan saveActivePlan(PlanSeed seed) {
        return subscriptionPlanRepository.save(SubscriptionPlan.builder()
            .name(seed.name())
            .tagline(seed.tagline())
            .priceVnd(seed.priceVnd())
            .periodType(seed.periodType())
            .periodCount(seed.periodCount())
            .maxTimePerAttemptMin(MAX_TIME_PER_ATTEMPT_MIN)
            // ACTIVE right away, not DRAFT: the purpose of this plan ladder is to be usable immediately, and with DRAFT
            // schools cannot subscribe. Different from SubscriptionPlan.create() -- that function is for
            // plans drafted by an administrator that must be approved before being sold.
            .status(SubscriptionPlanStatus.ACTIVE)
            .version(1L)
            .build());
    }

    // Grant EQUAL amounts to both types: no real data says which way the school's spending leans, and
    // guessing the ratio wrong would drain one wallet early while the other stays untouched -- and the two wallets cannot
    // be spent across each other.
    private void saveEqualQuotas(UUID planId, BigDecimal includedAmountVnd) {
        for (QuotaType quotaType : QUOTA_TYPES) {
            subscriptionPlanQuotaRepository.save(SubscriptionPlanQuota.builder()
                .subscriptionPlanId(planId)
                .quotaType(quotaType)
                .includedAmountVnd(includedAmountVnd)
                .build());
        }
    }

    /**
     * Một gói trong thang. HẠN MỨC suy ra từ ngân sách AI; GIÁ thì không -- hai con số này cố ý rời
     * nhau.
     *
     * <p><b>Hạn mức</b> = {@code includedQuotaUsd × }{@link #USD_TO_VND} phẳng, cấp bằng nhau cho mỗi
     * loại. Ngân sách USD chọn tròn và tỉ lệ thuận với độ dài chu kỳ (20 / 85 / 250 / 500 / 1.000 /
     * 2.000), nên mọi con số VNĐ đều là tích chẵn của 26.000 và tự kiểm được bằng nhẩm. Đây là con số
     * THẬT: nó quyết định trường chấm được bao nhiêu bài trước khi phải nạp thêm.
     *
     * <p><b>Giá</b> là một thang TƯỢNG TRƯNG cho môi trường demo, KHÔNG suy ra từ hạn mức. Bán đúng
     * bằng chi phí AI thì gói rẻ nhất đã hơn một triệu và gói dài nhất hơn trăm triệu -- không ai bấm
     * mua để xem thử luồng đăng ký, và cổng thanh toán sandbox cũng chỉ nên chạy với số tiền nhỏ. Vì
     * vậy thang gói này bán LỖ nặng so với hạn mức nó cấp, có chủ đích. Gói tạo từ giao diện quản trị
     * vẫn tính giá theo cách của nó, không dính gì tới đây.
     *
     * <p>Giá vẫn phải TĂNG NGHIÊM NGẶT theo độ dài chu kỳ, không được để bằng nhau: luồng đổi gói rẽ
     * nhánh bằng phép so giá -- đắt hơn thì hiệu lực ngay và bù phần chưa dùng, rẻ hơn thì xếp hàng
     * sau kỳ hiện tại. Sáu gói cùng giá là cả hai nhánh đó không bao giờ chạy tới, và cũng là bỏ mất
     * lý do để trường mua chu kỳ dài.
     *
     * <p>Biên lãi không nằm trong giá niêm yết ở đây: nó là dòng phí dịch vụ riêng trên đơn hàng, do
     * {@code ServiceFeePort} tính lúc đặt đơn -- xem {@code CreateSubscriptionOrderUseCase}. Nhờ vậy
     * sửa tỷ lệ phí không phải đụng vào thang gói, và trường đọc được rành mạch "tiền hàng bao nhiêu,
     * phí bao nhiêu".
     */
    private record PlanSeed(
        String name,
        String tagline,
        SubscriptionPlanPeriod periodType,
        int periodCount,
        BigDecimal includedQuotaUsd,
        BigDecimal priceVnd
    ) {

        /** The quota for EACH type (ASSESSMENT and PRACTICE receive equal amounts), converted to VND. */
        BigDecimal includedAmountVnd() {
            return includedQuotaUsd.multiply(USD_TO_VND);
        }
    }
}
