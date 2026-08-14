package com.sep.vox.application.port.output;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Đo độ trùng ngữ nghĩa giữa các câu ứng viên và các câu đã chọn, để đề luyện không lặp ý.
 * Implement bởi QuestionDiversityClient.
 */
public interface QuestionDiversityPort {

    /**
     * Độ tương đồng lớn nhất của từng câu ứng viên so với nhóm đã chọn. Dịch vụ AI chết thì trả
     * map RỖNG (không phải map toàn số 0) -- phía gọi hiểu là "không đo được" và dừng chọn thêm.
     */
    Map<UUID, Double> maxSimilarities(List<UUID> candidateIds, List<UUID> selectedIds);
}
