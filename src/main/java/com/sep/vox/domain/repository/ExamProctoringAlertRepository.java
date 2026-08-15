package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamProctoringAlert;

public interface ExamProctoringAlertRepository {

    /**
     * Ghi cảnh báo nếu {@code eventId} chưa từng được ghi, và báo lại có ghi mới hay không.
     *
     * <p>Trả về boolean thay vì void vì phía gọi cần phân biệt "đã lưu" với "đây là bản gửi lại":
     * cả hai đều thành công, nhưng chỉ một cái đáng ghi log là dữ liệu mới.
     */
    boolean saveIfAbsent(ExamProctoringAlert alert);

    List<ExamProctoringAlert> findByExamSessionIdOrderByCapturedAt(UUID examSessionId);

    /** Cảnh báo của cả ca thi, gộp qua mọi phiên thi trong ca -- cho màn giám sát trực tiếp. */
    List<ExamProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId);
}
