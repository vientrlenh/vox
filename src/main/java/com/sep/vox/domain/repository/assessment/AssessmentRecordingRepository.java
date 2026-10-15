package com.sep.vox.domain.repository.assessment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;

public interface AssessmentRecordingRepository {
    List<AssessmentRecording> findByExamSessionId(UUID examSessionId);

    /**
     * Mỗi nguồn ingest giữ hàng riêng của mình, nên source là một phần khoá: hai đường ingest của
     * cùng một phiên thi không tranh nhau một hàng nữa. source không được null -- xem
     * RecordingPartChangedCommandMapper.
     */
    Optional<AssessmentRecording> findByExamSessionIdAndStreamTypeAndSource(
        UUID examSessionId, AssessmentRequiredStreamType streamType, String source);

    AssessmentRecording save(AssessmentRecording recording);

    /**
     * Xoá bản ghi hình/tiếng của một phiên thi. Chỉ xoá dòng trong DB: file trên S3 để lifecycle
     * rule của bucket dọn, vì xoá file không rollback được cùng transaction.
     */
    void deleteByExamSessionId(UUID examSessionId);
}
