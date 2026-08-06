package com.sep.vox.application.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả gửi push, đã phân loại sẵn lỗi theo hành động mà phía gọi cần làm.
 *
 * <p>Phân loại nằm ở đây chứ không để phía gọi tự đọc mã lỗi của FCM, vì đây là chỗ
 * dễ gây mất dữ liệu: nếu gộp chung "cứ lỗi là xoá thiết bị" thì một lần FCM downtime
 * (UNAVAILABLE hàng loạt) sẽ quét sạch bảng notification_devices và toàn bộ người dùng
 * phải đăng ký lại thiết bị từ đầu.
 *
 * @param successCount               số thiết bị nhận được thông báo
 * @param staleInstallationIds       FID chắc chắn không còn dùng được -- phía gọi NÊN xoá khỏi DB
 * @param retryableInstallationIds   FID lỗi tạm thời -- phía gọi PHẢI giữ lại, thử lại sau
 */
public record PushDispatchResult(
    int successCount,
    List<String> staleInstallationIds,
    List<String> retryableInstallationIds
) {
    private static final PushDispatchResult EMPTY = new PushDispatchResult(0, List.of(), List.of());

    public PushDispatchResult {
        staleInstallationIds = staleInstallationIds == null ? List.of() : List.copyOf(staleInstallationIds);
        retryableInstallationIds = retryableInstallationIds == null ? List.of() : List.copyOf(retryableInstallationIds);
    }

    public static PushDispatchResult empty() {
        return EMPTY;
    }

    public int failureCount() {
        return staleInstallationIds.size() + retryableInstallationIds.size();
    }

    /** Gộp kết quả của các lô 500 thành một kết quả duy nhất cho phía gọi. */
    public PushDispatchResult merge(PushDispatchResult other) {
        if (other == null) {
            return this;
        }

        var stale = new ArrayList<>(this.staleInstallationIds);
        stale.addAll(other.staleInstallationIds);

        var retryable = new ArrayList<>(this.retryableInstallationIds);
        retryable.addAll(other.retryableInstallationIds);

        return new PushDispatchResult(this.successCount + other.successCount, stale, retryable);
    }
}
