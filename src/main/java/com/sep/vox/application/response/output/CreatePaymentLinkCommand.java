package com.sep.vox.application.response.output;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePaymentLinkCommand(
    String orderRef,
    BigDecimal amount,
    String description,
    /**
     * Hạn của link, LẤY TỪ orders.expires_at chứ không phải hằng số của adapter: link sống lâu hơn
     * đơn thì trường trả tiền cho một đơn đã hết hạn, còn ngắn hơn đơn thì đơn vẫn khóa chỗ mà
     * không còn cách nào trả.
     */
    Instant expiresAt
) {
    
}
