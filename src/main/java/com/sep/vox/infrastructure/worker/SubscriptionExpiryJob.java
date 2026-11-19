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
        Instant today = Instant.now();
        int expiredCount = schoolSubscriptionRepository.expireOverdue(today);
        if (expiredCount > 0) {
            LOGGER.info("Đã chuyển {} subscription quá hạn sang EXPIRED", expiredCount);
        }

        // The boundary between two periods is the ONLY place where the self-funded amount to carry over can be fixed, and this job is already
        // what runs at that boundary -- see CarryQuotaFundingAtPeriodStartService and V13. The order relative to the
        // sweep above does not matter: the carry-over attaches to the start_date of the NEW period, not to the status
        // of the old one, precisely because the new period can be spent right when its date arrives while the other sweep may lag by an hour.
        int carriedCount = carryQuotaFundingAtPeriodStartService.carryDueFunding(today);
        if (carriedCount > 0) {
            LOGGER.info("Đã mang tiền tự nạp chưa tiêu sang {} ví hạn mức của kỳ vừa bắt đầu", carriedCount);
        }
    }
}
