package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;

/**
 * Read side cho hai màn "nhìn lại": dòng thời gian điểm của một bài, và báo cáo chất
 * lượng AI của cả kỳ thi.
 *
 * <p>Cả hai đều join sang {@code users} để lấy tên người thao tác, nên đi lối query
 * repository thay vì domain repository.
 */
public interface ExamResultAuditQueryRepository {

    /** Dòng thời gian của một bài, cũ trước. */
    List<ResultStatusHistoryInfo> findHistory(UUID candidateResultId);

    /**
     * Báo cáo chất lượng AI trong phạm vi trường, lọc thêm theo kỳ thi nếu có.
     * Mẫu số là các vòng hậu kiểm đã hoàn thành.
     */
    AiQualityReportInfo aiQualityReport(UUID schoolId, UUID examId);
}
