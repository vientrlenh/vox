package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public SubscriptionExpiryJob(SchoolSubscriptionRepository schoolSubscriptionRepository) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void expireOverdueSubscriptions() {
        var today = Instant.now();
        var expiredCount = schoolSubscriptionRepository.expireOverdue(today);
        if (expiredCount > 0) {
            LOGGER.info("Đã chuyển {} subscription quá hạn sang EXPIRED", expiredCount);
        }
    }
}
