package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.UUID;

/**
 * Ảnh chụp trước khi admin chốt sổ cả kỳ thi.
 *
 * <p>Tồn tại vì {@code finalizeForPublish} bỏ qua bài {@code PENDING_REVIEW}: chỉ một
 * bài chưa ai chấm là cả kỳ thi không công bố được, mà trước đây admin không có cách
 * nào thấy bài đó là bài nào (review BE-5). Preview trả về đúng con số và danh sách
 * để họ quyết: chấm nốt, hay chấp nhận công bố theo điểm AI đang có.
 *
 * @param pendingUnassigned  bài chờ chấm và CHƯA có ai nhận
 * @param pendingAssigned    bài chờ chấm và đang có người cầm
 * @param openAppeals        đơn phúc khảo chưa kết thúc
 * @param invalid            bài đã bị vô hiệu (chốt sổ không đụng tới; sẽ thành FAILED
 *                           với điểm 0 khi kỳ thi công bố kết quả)
 * @param blockingResultIds  danh sách bài đang chặn, cắt ngắn cho UI
 */
public record BulkFinalizePreviewInfo(
    int total,
    int readyToFinalize,
    int pendingUnassigned,
    int pendingAssigned,
    int openAppeals,
    int invalid,
    List<UUID> blockingResultIds
) {
    /** Có thể chốt thẳng mà không cần admin chọn cách xử lý bài dở. */
    public boolean isClean() {
        return pendingUnassigned == 0 && pendingAssigned == 0 && openAppeals == 0;
    }
}
