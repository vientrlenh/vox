package com.sep.vox.domain.repository;

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
     * Does this session have any alert belonging to {@link AssessmentProctoringAlert#INTEGRITY_ALERT_TYPES} --
     * i.e. a reason to suspect the attempt was not done by the candidate themself.
     *
     * <p>Used when writing the grading result to push the attempt to waiting for a reviewer -- see
     * {@code RecordAssessmentAttemptEvaluationUseCase}. Returns a boolean and not a list because the caller
     * only needs to know whether there is one.
     *
     * <p>Asked by TYPE and not by {@code level} -- the full reason is in the javadoc of
     * {@code INTEGRITY_ALERT_TYPES}. In short: level is the scale of "what must the proctor do RIGHT NOW", this gate
     * asks "is the result trustworthy", and one adjustment of that scale has silently left this gate open.
     */
    boolean hasIntegrityAlert(UUID assessmentSessionId);

    /** Cảnh báo của cả ca thi, gộp qua mọi phiên thi trong ca -- cho màn giám sát trực tiếp. */
    List<AssessmentProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId);
}
