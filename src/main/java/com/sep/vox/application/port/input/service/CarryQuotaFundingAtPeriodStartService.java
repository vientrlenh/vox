package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

/**
 * Mang tiền tự nạp chưa tiêu của kỳ cũ sang kỳ mới, tại ĐÚNG lúc kỳ mới bắt đầu chạy.
 *
 * <p><b>Vì sao không làm luôn lúc chốt đơn.</b> {@code OrderSettlementService.seedQuotaRecords} vẫn
 * mang sang ngay khi hai mốc trùng nhau (gia hạn lúc kỳ cũ đã hết hạn -- đường đi thường gặp nhất).
 * Nhưng gia hạn SỚM thì kỳ mới bắt đầu ở endDate của kỳ cũ, cách lúc trả tiền có khi cả tháng, và
 * trong quãng đó kỳ CŨ vẫn là kỳ đang hiệu lực: vẫn tiêu được, vẫn nạp thêm được. Con số chốt tại
 * thời điểm trả tiền vì thế sai theo cả hai chiều -- trường tiêu nốt thì 5tr đó được tiêu HAI lần,
 * trường nạp thêm thì khoản mới bốc hơi ở ranh giới. Xem V13.
 *
 * <p><b>Vì sao CỘNG THÊM chứ không GÁN.</b> Kỳ mới có thể đã bắt đầu và trường đã tự nạp vào chính ví
 * đó trước khi job kịp chạy ({@code FundQuotaFromBalanceUseCase} cộng cả total lẫn funded). Một phép
 * gán "funded = phần chưa tiêu của kỳ cũ" sẽ xoá đúng khoản họ vừa bỏ ra. {@code addFundingFromBalance}
 * cộng thêm và giữ nguyên mọi thứ đã có.
 *
 * <p><b>Vì sao đúng một lần.</b> Cộng thêm mà chạy hai lần là nhân đôi tiền, nên cái hẹn
 * ({@code carry_funding_from_subscription_id}) bị xoá ngay sau khi cộng, trong CÙNG transaction. Job
 * chạy lại không thấy gì để làm; job chạy muộn chỉ làm tiền hiện ra muộn, không làm sai.
 *
 * <p>Không có bút toán nào sinh ra ở đây và đó là đúng: tiền này đã RỜI ví tự nạp từ lúc nạp, kèm một
 * dòng QUOTA_FUNDING. Đây chỉ là nó đi tiếp từ ví hạn mức kỳ cũ sang ví hạn mức kỳ mới, không đụng tới
 * {@code school_balances}, nên bất biến SUM(entries) = balance_vnd không liên quan.
 */
@Service
public class CarryQuotaFundingAtPeriodStartService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CarryQuotaFundingAtPeriodStartService.class);

    private final SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;

    public CarryQuotaFundingAtPeriodStartService(
            SchoolSubscriptionQuotaRecordRepository quotaRecordRepository) {
        this.quotaRecordRepository = quotaRecordRepository;
    }

    /**
     * @return số ví đã nhận tiền mang sang. 0 là trạng thái bình thường -- chỉ gia hạn sớm mới sinh hẹn.
     */
    @Transactional
    public int carryDueFunding(Instant now) {
        var due = quotaRecordRepository.findDueFundingCarries(now);
        if (due.isEmpty()) {
            return 0;
        }

        var carriedCount = 0;
        // Gom theo kỳ NGUỒN: mỗi nhóm chỉ cần đọc ví của kỳ đó một lần, và nhóm cũng chính là đơn vị
        // để phát hiện loại ví bị bỏ lại (xem reportStrandedTypes).
        var bySource = due.stream()
            .collect(Collectors.groupingBy(record -> record.getCarryFundingFromSubscriptionId()));

        for (var group : bySource.entrySet()) {
            var sourcePools = quotaRecordRepository.findBySchoolSubscriptionId(group.getKey());
            for (var target : group.getValue()) {
                if (carryOne(target, sourcePools, group.getKey())) {
                    carriedCount++;
                }
            }
            reportStrandedTypes(sourcePools, group.getValue(), group.getKey());
        }

        return carriedCount;
    }

    private boolean carryOne(SchoolSubscriptionQuotaRecord target,
            List<SchoolSubscriptionQuotaRecord> sourcePools, UUID sourceSubscriptionId) {
        var unspentVnd = sourcePools.stream()
            .filter(pool -> pool.getQuotaType() == target.getQuotaType())
            .findFirst()
            .map(record -> record.unspentFundedVnd())
            .orElse(BigDecimal.ZERO);

        // Xoá hẹn kể cả khi không có đồng nào để mang: hẹn còn treo là job còn đọc lại mỗi giờ, mãi mãi.
        if (unspentVnd.signum() <= 0) {
            quotaRecordRepository.clearFundingCarry(target.getId());
            return false;
        }

        quotaRecordRepository.addFundingFromBalance(target.getId(), unspentVnd);
        quotaRecordRepository.clearFundingCarry(target.getId());

        LOGGER.info("Kỳ {} bắt đầu: mang {}đ tiền tự nạp chưa tiêu của ví {} từ kỳ {} sang",
            target.getSchoolSubscriptionId(), unspentVnd, target.getQuotaType(), sourceSubscriptionId);
        return true;
    }

    /**
     * Kỳ nguồn còn tiền tự nạp ở một loại ví mà kỳ mới KHÔNG có -- gói mới không còn loại hạn mức đó.
     *
     * <p>Không có hẹn nào được đặt cho loại ví không tồn tại, nên nếu không nói ra ở đây thì khoản đó
     * nằm lại kỳ cũ và không ai biết. Cùng cảnh báo mà {@code seedQuotaRecords} phát ở đường mang sang
     * tức thì; ca này hiếm và chưa có đường xử lý tự động.
     */
    private void reportStrandedTypes(List<SchoolSubscriptionQuotaRecord> sourcePools,
            List<SchoolSubscriptionQuotaRecord> targets, UUID sourceSubscriptionId) {
        var carriedTypes = targets.stream()
            .map(record -> record.getQuotaType())
            .collect(Collectors.toSet());

        for (var pool : sourcePools) {
            var unspentVnd = pool.unspentFundedVnd();
            if (unspentVnd.signum() > 0 && !carriedTypes.contains(pool.getQuotaType())) {
                LOGGER.warn("Kỳ {} còn {}đ tiền tự nạp chưa tiêu ở ví {} nhưng kỳ kế tiếp không có loại"
                    + " hạn mức này -- khoản đó nằm lại và cần xử lý tay.",
                    sourceSubscriptionId, unspentVnd, pool.getQuotaType());
            }
        }
    }
}
