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

    /**
     * Phiên này có cảnh báo nào thuộc {@link ExamProctoringAlert#INTEGRITY_ALERT_TYPES} không --
     * tức có lý do nghi ngờ bài không phải do chính thí sinh làm.
     *
     * <p>Dùng lúc ghi kết quả chấm để đẩy bài sang chờ người soát -- xem
     * {@code RecordExamAttemptEvaluationUseCase}. Trả boolean chứ không trả danh sách vì nơi gọi
     * chỉ cần biết có hay không.
     *
     * <p>Hỏi theo LOẠI chứ không theo {@code level} -- lý do đầy đủ nằm ở javadoc của
     * {@code INTEGRITY_ALERT_TYPES}. Tóm tắt: level là thang "giám thị phải làm gì NGAY", cửa này
     * hỏi "kết quả có đáng tin không", và một lần chỉnh thang kia đã lặng lẽ làm cửa này hở.
     */
    boolean hasIntegrityAlert(UUID examSessionId);

    /** Cảnh báo của cả ca thi, gộp qua mọi phiên thi trong ca -- cho màn giám sát trực tiếp. */
    List<ExamProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId);
}
