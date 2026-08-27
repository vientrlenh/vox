package com.sep.vox.application.response.input.subscription;

import com.sep.vox.domain.dto.SubscriptionPlanDto;

/**
 * Hạn mức của gói KHÔNG nằm ở đây: chúng được resolver GraphQL nạp qua DataLoader, nên client nào
 * không chọn trường quotas thì không tốn thêm truy vấn nào.
 *
 * <p>Còn "phổ biến nhất" thì không thuộc về {@link SubscriptionPlanDto} vì nó không phải thuộc tính
 * của gói -- nó là kết quả so gói này với các gói khác, chỉ có nghĩa trong ngữ cảnh một danh sách.
 */
public record ViewSubscriptionPlansResponse(
    SubscriptionPlanDto subscription,
    boolean isMostPopular
) {

}
