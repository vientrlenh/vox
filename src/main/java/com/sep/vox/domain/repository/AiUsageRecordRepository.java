package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.AiUsageRecord;

public interface AiUsageRecordRepository {
    Optional<AiUsageRecord> findById(UUID id);
    AiUsageRecord save(AiUsageRecord record);
    List<AiUsageRecord> findByExamSessionId(UUID examSessionId);
    boolean existsByUsageEventId(UUID usageEventId);

    /**
     * GIÀNH mọi dòng chi phí CHƯA THU của phiên bằng cách đóng chung một mốc {@code chargedAt}, trả về
     * số dòng đã giành. Mốc đó là thẻ định danh của lượt thu này -- đọc lại bằng
     * {@link #sumCostVndByExamSessionIdAndChargedAt}.
     *
     * <p>Tồn tại vì một phiên được phép chấm LẠI (UpdateExamSessionStatusUseCase cho GRADED ->
     * GRADING): lần chấm sau sinh chi phí thật mới và phải được thu, nhưng nếu cứ cộng cả phiên thì
     * phần của lần chấm trước bị thu tiền lần thứ hai. Đơn vị "chỉ được thu một lần" là TỪNG DÒNG chi
     * phí, không phải phiên thi.
     *
     * <p>Giành trước rồi cộng sau: theo chiều ngược lại, một dòng usage do Kafka chèn vào giữa hai
     * bước sẽ bị đóng dấu đã thu mà chưa từng được cộng vào khoản trừ -- mất trắng khoản đó.
     */
    int markChargedByExamSessionId(UUID examSessionId, Instant chargedAt);

    /**
     * MIỄN mọi dòng chi phí chưa ngã ngũ của phiên — chúng thuộc lượt chấm vừa hỏng.
     *
     * <p>Quy tắc là "thu cho phần việc TẠO RA KẾT QUẢ DÙNG ĐƯỢC", không phải "hỏng thì miễn": một
     * lượt chấm hỏng không để lại dòng {@code exam_candidate_results} nào nên trường không nhận được
     * gì. Đừng suy diễn quy tắc này sang đường luyện nói, nơi tiền được thu ngay trong request theo
     * chi phí đã phát sinh vì lượt nói đó ĐÃ trả kết quả cho học sinh — xem
     * {@code SubmitPracticeTurnUseCase}.
     *
     * @return số dòng vừa được miễn
     */
    int markWaivedByExamSessionId(UUID examSessionId, Instant waivedAt);

    /**
     * Tổng cost_vnd của những dòng vừa được {@link #markChargedByExamSessionId} giành ở mốc này --
     * nguồn thật để trừ SchoolSubscriptionQuotaRecord.
     *
     * <p>Cộng cost_vnd đã chốt sẵn từng dòng chứ KHÔNG quy đổi tổng USD theo tỷ giá hôm nay: mỗi dòng
     * đã ghi tỷ giá đúng lúc chi phí phát sinh (fx_rate_used), nên một phiên thi vắt qua ngày đổi tỷ
     * giá vẫn ra đúng số tiền thật, và cộng lại lần nữa cũng không cho ra con số khác.
     */
    BigDecimal sumCostVndByExamSessionIdAndChargedAt(UUID examSessionId, Instant chargedAt);

    /**
     * Như trên nhưng giữ nguyên tệ gốc, để ghi vào school_balance_entries.cost_usd cho việc đối soát
     * ngược với hóa đơn nhà cung cấp. Phải soi CÙNG mốc chargedAt với bản VND, nếu không hai cột của
     * cùng một bút toán sẽ mô tả hai tập dòng khác nhau.
     */
    BigDecimal sumCostUsdByExamSessionIdAndChargedAt(UUID examSessionId, Instant chargedAt);
}
