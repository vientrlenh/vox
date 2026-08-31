package com.sep.vox.application.query.repository;

import java.time.Instant;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.domain.common.PageResult;

/**
 * Danh sách trường trong một nhóm "cần chú ý" — chỗ đáp của bốn dòng trên thẻ cùng tên ở trang tổng
 * quan hệ thống.
 *
 * <p>KHÔNG dùng lại màn "Trường &amp; gói" có sẵn cho việc này, dù nó cũng lọc theo trạng thái: màn
 * đó lọc theo DÒNG thuê bao còn thẻ đếm theo TRƯỜNG. Một trường có kỳ 2024 đã hết và kỳ 2025 đang
 * chạy sẽ nằm trong danh sách {@code status=EXPIRED} của màn đó nhưng KHÔNG thuộc nhóm "đã hết hạn"
 * — bấm vào thẻ ghi 5 rồi thấy 9 dòng thì không con số nào còn đáng tin.
 *
 * <p>Vì thế vị từ ở đây là bản sao ĐÚNG TỪNG CHỮ của
 * {@link PlatformBusinessHealthQueryRepository#countSchoolSubscriptionHealth}: gộp về một dòng mỗi
 * trường trước, rồi mới phân nhóm.
 */
public interface SchoolsAtRiskQueryRepository {

    /**
     * @param now             thời điểm xét kỳ thuê bao nào đang phủ — truyền cùng giá trị đã dùng để
     *                        đếm, nếu không thì một kỳ hết hạn giữa hai lần gọi sẽ làm số đếm và
     *                        danh sách lệch nhau
     * @param expiringThrough ngưỡng cảnh báo sắp hết hạn, BAO GỒM; chỉ dùng cho
     *                        {@link SchoolRiskBucket#EXPIRING_SOON}
     */
    PageResult<SchoolAtRiskDto> findByBucket(
        SchoolRiskBucket bucket, Instant now, Instant expiringThrough, String keyword, int page, int size);
}
