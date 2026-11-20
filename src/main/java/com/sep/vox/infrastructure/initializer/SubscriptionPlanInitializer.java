package com.sep.vox.infrastructure.initializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

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
 * Dựng sẵn một THANG gói đăng ký ACTIVE để trường mới đăng ký được ngay, không phải tạo tay trước.
 *
 * <p>Sáu gói trải đủ ba loại chu kỳ: một gói dùng thử 7 ngày, ba gói tháng (1/3/6) và hai gói năm
 * (1/2). Một gói duy nhất như bản trước đủ để đăng ký, nhưng KHÔNG đủ để chạy thử luồng đổi gói:
 * quyết định "hiệu lực ngay và bù phần chưa dùng" hay "xếp hàng sau kỳ hiện tại" dựa trên phép so
 * GIÁ giữa gói mới và gói đang chạy, mà một gói thì không có gì để so.
 *
 * <p><b>Bỏ qua theo TỪNG GÓI, không phải cả lượt.</b> Bản trước bỏ qua toàn bộ nếu đã tồn tại bất kỳ
 * gói ACTIVE nào, nghĩa là một cơ sở dữ liệu đang chạy sẽ không bao giờ nhận được các gói mới thêm
 * vào danh sách này. Giờ mỗi gói được đối chiếu theo TÊN, nên thêm một gói vào {@link #PLANS} là nó
 * xuất hiện ở lần khởi động kế tiếp mà không đụng gì tới các gói đã có.
 *
 * <p>Đối chiếu trên MỌI trạng thái chứ không riêng ACTIVE: quản trị viên lưu trữ (ARCHIVED) một gói
 * là một quyết định, và dựng lại nó ở lần khởi động sau là ghi đè lên quyết định đó.
 *
 * <p><b>Hạn mức đo bằng TIỀN.</b> Từ V2, {@code plan_quota.included_quantity} đổi thành
 * {@code subscription_plan_quotas.included_amount_vnd} và {@code token_unit_price} bị bỏ hẳn -- lớp
 * này vì thế không cần {@code QuotaPricingPort}, không còn con số đơn giá nào để tra.
 */
@Component
@Order(10)
public class SubscriptionPlanInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionPlanInitializer.class);

    /**
     * Tỷ giá quy đổi PHẲNG, không cộng thêm gì.
     *
     * <p>Đúng bằng {@code DEFAULT_USD_TO_VND_RATE} của {@code QuotaSellingPriceProperties}: tỷ giá
     * thị trường, KHÔNG nhân với {@code (1 + serviceFeeRatio)}. Nhân sẵn biên lãi vào tỷ giá chính là
     * thứ V2 đã dẹp -- nó tạo ra một "đơn giá" mà trường không đối chiếu được với bất kỳ tỷ giá nào
     * bên ngoài. Phí dịch vụ chỉ được xuất hiện dưới dạng một dòng phí riêng trên đơn hàng, do
     * {@code ServiceFeePort} quyết định lúc đặt đơn.
     */
    private static final BigDecimal USD_TO_VND = new BigDecimal("26000");

    /** V2 gộp GRADING và CLASS_TEST vào EXAM, nên chỉ còn hai loại hạn mức. */
    private static final List<QuotaType> QUOTA_TYPES = List.of(QuotaType.EXAM, QuotaType.PRACTICE);

    /**
     * Trần độ dài MỘT lượt thi, dùng chung cho cả thang gói. Đây là cái thước, không phải túi tiền --
     * nó không cạn đi khi thi nhiều, chỉ chặn mã đề dài quá mức. Để 60 phút cho rộng hơn hẳn mã đề
     * demo (khoảng 20-25 phút, đã gồm thời lượng audio/video) để không chặn nhầm lúc soạn đề.
     */
    private static final int MAX_TIME_PER_ATTEMPT_MIN = 60;

    /** Đủ lớn để gom hết danh mục gói trong một lượt đọc -- danh mục này đếm bằng chục, không phải nghìn. */
    private static final int LOOKUP_PAGE_SIZE = 500;

    /**
     * THỨ TỰ TRONG DANH SÁCH NÀY LÀ CÓ CHỦ ĐÍCH: xếp từ ĐẮT xuống RẺ.
     *
     * <p>Danh sách gói trả về cho trường sắp theo {@code id DESC} (xem
     * {@code SubscriptionPlanRepositoryImpl.NEWEST_FIRST}), mà id là uuidv7 nên tăng dần theo thứ tự
     * chèn. Chèn từ đắt xuống rẻ vì thế cho ra màn chọn gói đọc từ rẻ lên đắt -- đúng cách một bảng
     * giá được đọc.
     *
     * <p>Đây là ràng buộc MỀM, không phải bảo đảm: hai gói chèn trong cùng một mili giây có thể đảo
     * chỗ. Muốn chắc chắn thì phải cho đường đọc sắp theo giá, và đó là một thay đổi hành vi riêng.
     *
     * <p>Giá đi kèm là thang DEMO (10k → 280k), cố ý rẻ hơn hẳn chi phí AI mà gói cấp -- xem
     * {@link PlanSeed}. Hạn mức thì vẫn là con số thật, quy từ ngân sách USD theo tỷ giá phẳng.
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

    public SubscriptionPlanInitializer(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var existingNames = subscriptionPlanRepository.findAll(1, LOOKUP_PAGE_SIZE).content().stream()
            .map(plan -> plan.getName())
            .collect(Collectors.toSet());

        var created = 0;
        for (var seed : PLANS) {
            if (existingNames.contains(seed.name())) {
                continue;
            }
            createPlan(seed);
            created++;
        }

        if (created == 0) {
            LOGGER.info("Subscription plan catalogue already complete ({} plans). Skip initialize", PLANS.size());
            return;
        }

        LOGGER.info("Subscription plan catalogue initialized: {} of {} plans created", created, PLANS.size());
    }

    private void createPlan(PlanSeed seed) {
        var now = Instant.now();
        var includedAmountVnd = seed.includedAmountVnd();

        var plan = subscriptionPlanRepository.save(new SubscriptionPlan(
            seed.name(),
            seed.tagline(),
            seed.priceVnd(),
            seed.periodType(),
            seed.periodCount(),
            MAX_TIME_PER_ATTEMPT_MIN,
            // ACTIVE ngay, không phải DRAFT: mục đích của thang gói này là dùng được luôn, mà DRAFT
            // thì trường không đăng ký được. Khác với SubscriptionPlan.create() -- hàm đó dành cho
            // gói do quản trị viên soạn và phải duyệt trước khi bán.
            SubscriptionPlanStatus.ACTIVE,
            1L,
            now,
            now,
            // Không có người dùng đăng nhập lúc khởi động; cột created_by/updated_by cho phép null.
            null,
            null
        ));

        // Cấp BẰNG NHAU cho cả hai loại: chưa có số liệu thật nào nói trường tiêu lệch về bên nào, và
        // đoán sai tỷ lệ sẽ làm một ví cạn sớm trong khi ví kia còn nguyên -- mà hai ví không tiêu
        // chéo được cho nhau.
        for (var quotaType : QUOTA_TYPES) {
            subscriptionPlanQuotaRepository.save(new SubscriptionPlanQuota(
                plan.getId(),
                quotaType,
                includedAmountVnd
            ));
        }

        LOGGER.info(
            "Subscription plan created: {} ({} {}, gia {}d, han muc {}d moi loai)",
            seed.name(),
            seed.periodCount(),
            seed.periodType(),
            seed.priceVnd().toPlainString(),
            includedAmountVnd.toPlainString()
        );
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

        /** Hạn mức cho MỖI loại (EXAM và PRACTICE nhận bằng nhau), quy ra VNĐ. */
        BigDecimal includedAmountVnd() {
            return includedQuotaUsd.multiply(USD_TO_VND);
        }
    }
}
