package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Chia một tập việc cho một nhóm người theo <em>tải thật</em>, không phải chia vòng
 * tròn từ số 0.
 *
 * <p>Điểm xuất phát là số việc mỗi người đang giữ, nên chạy lần thứ hai không dồn hết
 * vào người đầu danh sách — đó là toàn bộ lý do lớp này tồn tại và là phần đáng giữ
 * nhất của bản mở rộng cũ.
 */
@Service
public class RoundRobinLoadBalancer {

    /**
     * Trả về giáo viên được chọn cho từng việc, theo đúng thứ tự việc đưa vào.
     *
     * @param currentLoads tải hiện tại; thiếu ai thì coi như 0
     * @throws IllegalArgumentException khi không có ai để chia
     */
    public List<UUID> distribute(List<UUID> workItems, List<UUID> assigneeIds, Map<UUID, Long> currentLoads) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một giáo viên để phân công.");
        }
        if (workItems == null || workItems.isEmpty()) {
            return List.of();
        }

        var loads = new long[assigneeIds.size()];
        for (var index = 0; index < assigneeIds.size(); index++) {
            loads[index] = currentLoads == null ? 0L : currentLoads.getOrDefault(assigneeIds.get(index), 0L);
        }

        var picked = new ArrayList<UUID>(workItems.size());
        for (var item = 0; item < workItems.size(); item++) {
            var target = leastLoadedIndex(loads);
            loads[target]++;
            picked.add(assigneeIds.get(target));
        }
        return picked;
    }

    /** Hoà thì người đứng trước thắng — số việc dư rải tuần tự theo thứ tự đã chọn. */
    private int leastLoadedIndex(long[] loads) {
        var best = 0;
        for (var index = 1; index < loads.length; index++) {
            if (loads[index] < loads[best]) {
                best = index;
            }
        }
        return best;
    }
}
