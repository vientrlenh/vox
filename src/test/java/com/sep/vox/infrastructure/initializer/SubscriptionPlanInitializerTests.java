package com.sep.vox.infrastructure.initializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Thang gói khởi tạo là thứ mọi buổi demo bắt đầu từ đó, nên hai điều phải chắc: nó dựng ĐỦ sáu gói
 * với đúng con số, và chạy lại lần thứ hai KHÔNG nhân đôi danh mục.
 *
 * <p>Initializer là {@code ApplicationRunner} nên nó đã chạy xong lúc context khởi động -- test này
 * chỉ soi kết quả, rồi gọi {@code run()} thêm một lần nữa để kiểm tính bất biến.
 *
 * <p>KHÔNG có {@code @Transactional}: dữ liệu được ghi lúc khởi động context, ngoài mọi transaction
 * của test, nên gói một transaction quanh test chẳng dọn được gì mà chỉ gây hiểu nhầm.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
class SubscriptionPlanInitializerTests extends ContainerTestConfig {

    /** Ngân sách AI mỗi loại hạn mức, tính bằng USD -- nguồn duy nhất sinh ra cả hạn mức lẫn giá. */
    private static final Map<String, Integer> EXPECTED_QUOTA_USD = Map.of(
        "Gói dùng thử 7 ngày", 20,
        "Gói 1 tháng", 85,
        "Gói 3 tháng", 250,
        "Gói 6 tháng", 500,
        "Gói 1 năm", 1_000,
        "Gói 2 năm", 2_000
    );

    private static final Map<String, SubscriptionPlanPeriod> EXPECTED_PERIOD_TYPE = Map.of(
        "Gói dùng thử 7 ngày", SubscriptionPlanPeriod.DAY,
        "Gói 1 tháng", SubscriptionPlanPeriod.MONTH,
        "Gói 3 tháng", SubscriptionPlanPeriod.MONTH,
        "Gói 6 tháng", SubscriptionPlanPeriod.MONTH,
        "Gói 1 năm", SubscriptionPlanPeriod.YEAR,
        "Gói 2 năm", SubscriptionPlanPeriod.YEAR
    );

    private static final Map<String, Integer> EXPECTED_PERIOD_COUNT = Map.of(
        "Gói dùng thử 7 ngày", 7,
        "Gói 1 tháng", 1,
        "Gói 3 tháng", 3,
        "Gói 6 tháng", 6,
        "Gói 1 năm", 1,
        "Gói 2 năm", 2
    );

    /** Thang giá DEMO -- cố ý rẻ hơn hẳn chi phí AI mà gói cấp, xem javadoc của PlanSeed. */
    private static final Map<String, Integer> EXPECTED_PRICE_VND = Map.of(
        "Gói dùng thử 7 ngày", 10_000,
        "Gói 1 tháng", 20_000,
        "Gói 3 tháng", 50_000,
        "Gói 6 tháng", 90_000,
        "Gói 1 năm", 160_000,
        "Gói 2 năm", 280_000
    );

    private static final BigDecimal USD_TO_VND = new BigDecimal("26000");

    @Autowired
    private SubscriptionPlanInitializer initializer;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    @Test
    void should_seed_every_plan_in_the_ladder_as_active() {
        var byName = activePlansByName();

        assertThat(byName.keySet()).containsAll(EXPECTED_QUOTA_USD.keySet());
    }

    @Test
    void should_derive_quota_from_a_flat_rate_with_no_markup() {
        var byName = activePlansByName();

        EXPECTED_QUOTA_USD.forEach((name, quotaUsd) -> {
            var plan = byName.get(name);
            assertThat(plan).as("gói %s", name).isNotNull();

            // Tỷ giá PHẲNG 26.000, không nhân (1 + serviceFeeRatio). Biên lãi nằm ở dòng phí dịch vụ
            // trên đơn hàng, không được nhét sẵn vào tỷ giá -- đó chính là thứ V2 đã dẹp.
            var expectedQuotaVnd = BigDecimal.valueOf(quotaUsd).multiply(USD_TO_VND);

            assertThat(plan.getPeriodType()).as("loại chu kỳ %s", name).isEqualTo(EXPECTED_PERIOD_TYPE.get(name));
            assertThat(plan.getPeriodCount()).as("số chu kỳ %s", name).isEqualTo(EXPECTED_PERIOD_COUNT.get(name));

            var quotas = subscriptionPlanQuotaRepository.findBySubscriptionPlanId(plan.getId());
            assertThat(quotas).as("hạn mức gói %s", name).hasSize(2);
            assertThat(quotas).extracting(q -> q.getQuotaType())
                .containsExactlyInAnyOrder(QuotaType.EXAM, QuotaType.PRACTICE);
            // Hai loại nhận BẰNG NHAU -- hai ví không tiêu chéo được nên đoán lệch tỷ lệ là làm một
            // ví cạn sớm trong khi ví kia còn nguyên.
            for (var quota : quotas) {
                assertThat(quota.getIncludedAmountVnd())
                    .as("hạn mức %s của gói %s", quota.getQuotaType(), name)
                    .isEqualByComparingTo(expectedQuotaVnd);
            }
        });
    }

    @Test
    void should_price_the_ladder_cheaply_enough_to_pay_in_a_sandbox() {
        var byName = activePlansByName();

        EXPECTED_PRICE_VND.forEach((name, price) -> {
            var plan = byName.get(name);
            assertThat(plan).as("gói %s", name).isNotNull();
            assertThat(plan.getPriceVnd())
                .as("giá gói %s", name)
                .isEqualByComparingTo(BigDecimal.valueOf(price));

            // Giá KHÔNG được bám theo hạn mức: bán đúng bằng chi phí AI thì gói rẻ nhất đã hơn một
            // triệu, và không ai bấm mua để xem thử luồng đăng ký.
            var quotaVnd = BigDecimal.valueOf(EXPECTED_QUOTA_USD.get(name)).multiply(USD_TO_VND);
            assertThat(plan.getPriceVnd())
                .as("gói %s phải bán rẻ hơn hẳn hạn mức nó cấp", name)
                .isLessThan(quotaVnd);
        });
    }

    @Test
    void should_not_duplicate_the_catalogue_when_run_again() throws Exception {
        var before = activePlansByName().size();

        initializer.run(new DefaultApplicationArguments());

        assertThat(activePlansByName()).as("chạy lại không được nhân đôi danh mục").hasSize(before);
    }

    @Test
    void should_price_the_ladder_strictly_upwards_so_both_upgrade_branches_are_reachable() {
        var byName = activePlansByName();

        // Luồng đổi gói rẽ nhánh theo phép so GIÁ: đắt hơn thì hiệu lực ngay và bù phần chưa dùng,
        // rẻ hơn thì xếp hàng sau kỳ hiện tại. Thang giá phải tăng nghiêm ngặt thì cả hai nhánh mới
        // chạy thử được -- hai gói cùng giá là một nhánh không bao giờ tới.
        var ladder = new String[] {
            "Gói dùng thử 7 ngày", "Gói 1 tháng", "Gói 3 tháng", "Gói 6 tháng", "Gói 1 năm", "Gói 2 năm",
        };

        for (var i = 1; i < ladder.length; i++) {
            var cheaper = byName.get(ladder[i - 1]).getPriceVnd();
            var pricier = byName.get(ladder[i]).getPriceVnd();
            assertThat(pricier)
                .as("%s phải đắt hơn %s", ladder[i], ladder[i - 1])
                .isGreaterThan(cheaper);
        }
    }

    private Map<String, SubscriptionPlan> activePlansByName() {
        return subscriptionPlanRepository.findByStatus(SubscriptionPlanStatus.ACTIVE).stream()
            .collect(java.util.stream.Collectors.toMap(plan -> plan.getName(), plan -> plan, (a, b) -> a));
    }
}
