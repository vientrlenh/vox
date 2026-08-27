package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Trần CẢNH BÁO cho nợ của trường -- xem ConsumeQuotaService.checkDebtCapTransition. Giá trị thật
 * bind từ application.yaml ({@code vox.quota.debt.*}) ở QuotaDebtProperties.
 */
public interface QuotaDebtConfigPort {

    /**
     * Nợ được coi là bất thường khi vượt {@code total_allocated_amount_vnd * capRatio} của ví hạn
     * mức đã gây ra nó. Lấy theo TỶ LỆ trên hạn mức chứ không phải một số tiền tuyệt đối: trường mua
     * gói lớn thì một khoản nợ vài trăm nghìn là chuyện thường, còn trường mua gói nhỏ thì chính
     * khoản đó là dấu hiệu pipeline đo chi phí AI có bug.
     */
    BigDecimal capRatio();
}
