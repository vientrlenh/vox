package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Cổng ĐỌC tỷ lệ phí dịch vụ cho tầng application/interfaces. Nguồn số thật là config toàn hệ thống
 * ({@code vox.quota.selling-price.service-fee-ratio}) nằm ở infrastructure -- xem QuotaPricingService.
 *
 * <p>Tách khỏi {@link QuotaPricingPort} một cách CÓ CHỦ Ý dù cùng một implementation: port kia trả về
 * GIÁ VỐN (chi phí AI ước tính mỗi giây, tỷ giá USD->VND) và được phơi nguyên vẹn ra query GraphQL
 * {@code quotaPricing} cho trường xem. Còn con số này là BIÊN LÃI của mình -- nó chỉ được xuất hiện
 * dưới dạng đã cộng vào một dòng phí trên đơn hàng, không phải một trường tra cứu được.
 *
 * <p>Nhờ tách mà use case đặt đơn nạp tiền chỉ phụ thuộc đúng con số nó cần, thay vì kéo theo cả
 * chi phí thi/luyện tập mà nó không bao giờ đọc.
 *
 * <p>Implementation tự xử lý fallback bên trong (config .env -> hằng số mặc định) nên luôn trả về
 * giá trị dùng được, không bao giờ null.
 */
public interface ServiceFeePort {

    /**
     * Tỷ lệ phí dịch vụ (vd. 0.20 = 20%) mà ĐƠN HÀNG cộng thêm vào phần tiền hạn mức.
     *
     * <p>CỐ Ý không gộp sẵn vào tỷ giá: gộp thì ra "1 USD = 31.200đ" trong khi tỷ giá thật là
     * 26.000đ, trường không đối chiếu được với bất kỳ tỷ giá nào ngoài thị trường. Phí phải đứng
     * thành một dòng riêng trên đơn để nhìn là biết mình đang trả cái gì.
     *
     * <p>Là config TOÀN HỆ THỐNG chứ không lưu theo từng gói (cột service_fee_ratio đã bị bỏ khỏi
     * SubscriptionPlan) -- xem QuotaSellingPriceProperties.
     */
    BigDecimal serviceFeeRatio();
}
