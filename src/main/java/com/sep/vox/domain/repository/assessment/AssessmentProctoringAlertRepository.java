package com.sep.vox.domain.repository.assessment;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;

public interface AssessmentProctoringAlertRepository {

    /**
     * Ghi cảnh báo nếu {@code eventId} chưa từng được ghi, và báo lại có ghi mới hay không.
     *
     * <p>Trả về boolean thay vì void vì phía gọi cần phân biệt "đã lưu" với "đây là bản gửi lại":
     * cả hai đều thành công, nhưng chỉ một cái đáng ghi log là dữ liệu mới.
     */
    boolean saveIfAbsent(AssessmentProctoringAlert alert);

    List<AssessmentProctoringAlert> findByAssessmentSessionIdOrderByCapturedAt(UUID assessmentSessionId);

    /**
     * Phiên này có cảnh báo nào thuộc {@link AssessmentProctoringAlert#INTEGRITY_ALERT_TYPES} không --
     * tức có lý do nghi ngờ bài không phải do chính thí sinh làm.
     *
     * <p>Dùng lúc ghi kết quả chấm để đẩy bài sang chờ người soát -- xem
     * {@code RecordAssessmentAttemptEvaluationUseCase}. Trả boolean chứ không trả danh sách vì nơi gọi
     * chỉ cần biết có hay không.
     *
     * <p>Hỏi theo LOẠI chứ không theo {@code level} -- lý do đầy đủ nằm ở javadoc của
     * {@code INTEGRITY_ALERT_TYPES}. Tóm tắt: level là thang "giám thị phải làm gì NGAY", cửa này
     * hỏi "kết quả có đáng tin không", và một lần chỉnh thang kia đã lặng lẽ làm cửa này hở.
     */
    boolean hasIntegrityAlert(UUID assessmentSessionId);

    /** Cảnh báo của cả ca thi, gộp qua mọi phiên thi trong ca -- cho màn giám sát trực tiếp. */
    List<AssessmentProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId);
}
