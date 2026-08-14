package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeSession;

public interface PracticeSessionRepository {

    Optional<PracticeSession> findById(UUID id);

    Optional<PracticeSession> findByIdForUpdate(UUID id);

    boolean existsByIdAndStudentIdAndStatus(UUID id, UUID studentId, String status);

    PracticeSession save(PracticeSession session);

    List<PracticeSession> findStaleInProgress(Instant staleBefore);

    void refreshOverallScore(UUID sessionId);

    /**
     * Đánh dấu phiên VẪN CÒN SỐNG -- đặt {@code last_heartbeat_at} về hiện tại.
     *
     * <p>Trước 2026-08-12 cột này chỉ được ghi ĐÚNG MỘT LẦN lúc tạo phiên rồi không nơi nào cập
     * nhật (setter có nhưng không ai gọi). Mà PracticeSessionHeartbeatCleanupJob đóng mọi phiên
     * có nhịp cũ hơn ngưỡng, và job chạy mỗi 30 giây -- nên MỌI phiên luyện đều bị đóng ở khoảng
     * phút thứ 3, bất kể học sinh đang nói. Sau đó mọi lượt nộp trả 404 "Phiên luyện không còn
     * hoạt động" (đo được trên production: phiên 681cdd72 mở 09:46:48, nộp lượt 09:52:11 -> 404).
     *
     * <p>Chỉ UPDATE một cột chứ không nạp entity rồi save: chạy trên đường realtime nóng, và
     * không được ghi đè các cột mà luồng chấm đang cập nhật song song.
     */
    void touchHeartbeat(UUID sessionId);
}
