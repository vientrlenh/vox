package com.sep.vox.application.query.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AiCostBucketDto;
import com.sep.vox.application.query.dto.UserAiSpendDto;
import com.sep.vox.domain.common.PageResult;

/**
 * Đọc sổ {@code school_ai_spend_entries} — chi phí AI mà TRƯỜNG bị trừ.
 *
 * <p>Thay cho {@code TokenUsageTimeseriesQueryRepository} đã xoá: bản cũ cộng {@code tokens_consumed}
 * trên {@code token_usage_events}, một bảng không mã nào còn ghi vào kể từ V2, và đơn vị của nó cũng
 * không còn là đơn vị hệ thống dùng để tính tiền.
 *
 * <p>Cả hai câu đều gom nhóm ở DB. Nạp từng dòng về Java rồi cộng là kéo hàng vạn dòng để in ra ba
 * mươi con số.
 */
public interface SchoolAiCostQueryRepository {

    /**
     * @param granularityUnit đơn vị của {@code date_trunc}: {@code day} / {@code week} / {@code month}.
     *                        Cắt theo giờ NGHIỆP VỤ, không phải UTC — nếu không thì 7 giờ đầu mỗi
     *                        ngày rơi sang ngày hôm trước trên biểu đồ.
     */
    List<AiCostBucketDto> findBucketedCost(UUID schoolId, Instant from, Instant to, String granularityUnit);

    /**
     * Ai đã tiêu bao nhiêu trong cửa sổ, nhiều nhất trước.
     *
     * <p>CHỈ những dòng có người. Khoản của kỳ thi tập trung mang {@code user_id} null vì nó thuộc về
     * cả trường, và gộp chúng thành một hàng "không rõ" sẽ đứng đầu bảng ở gần như mọi trường — con
     * số đó thuộc về thẻ tổng, không thuộc bảng xếp hạng người dùng.
     *
     * @param quotaType lọc theo loại ví; null là cả hai.
     */
    PageResult<UserAiSpendDto> findSpendByUser(
        UUID schoolId, Instant from, Instant to, String quotaType, int page, int size);

    /**
     * Mốc sổ bắt đầu có dữ liệu của trường; null khi trường chưa từng tiêu đồng nào.
     *
     * <p>V10 cố ý KHÔNG backfill, nên mọi khoảng nằm trước mốc này vẽ ra một đường phẳng ở 0 dù
     * trường có tiêu tiền thật. Trả con số ra để giao diện nói được sự khác biệt — không có nó thì
     * "tháng trước không tiêu gì" và "tháng trước chưa ghi sổ" trông y hệt nhau.
     */
    Instant findFirstRecordedAt(UUID schoolId);

    /**
     * Phần chi KHÔNG thuộc trần chi của ai trong cửa sổ — tức khoản của kỳ thi tập trung.
     *
     * <p>Đi kèm {@link #findSpendByUser}: cộng bảng người dùng rồi so với biểu đồ sẽ luôn thiếu đúng
     * con số này, và thiếu mà không giải thích thì trông như thất thoát.
     */
    BigDecimal sumSchoolWideCost(UUID schoolId, Instant from, Instant to, String quotaType);
}
