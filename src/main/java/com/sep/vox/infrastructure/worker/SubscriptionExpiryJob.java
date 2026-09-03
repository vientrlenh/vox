package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.CarryQuotaFundingAtPeriodStartService;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// Trước đây KHÔNG có gì tự động đưa subscription ACTIVE quá endDate sang EXPIRED — status chỉ đổi
// khi có subscription MỚI đè lên (gia hạn / admin duyệt request khác). Job này lấp đúng chỗ đó, và
// cũng là cơ chế "cắt quyền" cho case hủy giữa chừng kiểu Claude: hủy chỉ set cancelledAt (xem
// CancelSubscriptionUseCase), subscription vẫn ACTIVE/dùng được bình thường tới hết endDate, rồi
// job này tự chuyển EXPIRED khi ngày đó tới — không cần biết subscription có bị hủy hay không, cứ
// hết hạn là hết hạn.
@Component
public class SubscriptionExpiryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionExpiryJob.class);

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final CarryQuotaFundingAtPeriodStartService carryQuotaFundingAtPeriodStartService;

    public SubscriptionExpiryJob(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            CarryQuotaFundingAtPeriodStartService carryQuotaFundingAtPeriodStartService) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.carryQuotaFundingAtPeriodStartService = carryQuotaFundingAtPeriodStartService;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void expireOverdueSubscriptions() {
        var today = Instant.now();
        var expiredCount = schoolSubscriptionRepository.expireOverdue(today);
        if (expiredCount > 0) {
            LOGGER.info("Đã chuyển {} subscription quá hạn sang EXPIRED", expiredCount);
        }

        // Ranh giới hai kỳ là chỗ DUY NHẤT chốt được phần tiền tự nạp mang sang, và job này vốn đã là
        // thứ chạy ở ranh giới đó -- xem CarryQuotaFundingAtPeriodStartService và V13. Thứ tự với lần
        // quét trên không quan trọng: phép mang sang bám vào start_date của kỳ MỚI, không bám status
        // của kỳ cũ, đúng vì kỳ mới tiêu được ngay khi tới ngày còn lần quét kia có thể chậm cả giờ.
        var carriedCount = carryQuotaFundingAtPeriodStartService.carryDueFunding(today);
        if (carriedCount > 0) {
            LOGGER.info("Đã mang tiền tự nạp chưa tiêu sang {} ví hạn mức của kỳ vừa bắt đầu", carriedCount);
        }
    }
}
