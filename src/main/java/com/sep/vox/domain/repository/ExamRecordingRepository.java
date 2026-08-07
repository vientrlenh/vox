package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;

public interface ExamRecordingRepository {
    List<ExamRecording> findByExamSessionId(UUID examSessionId);

    /**
     * Mỗi nguồn ingest giữ hàng riêng của mình, nên source là một phần khoá: hai đường ingest của
     * cùng một phiên thi không tranh nhau một hàng nữa. source không được null -- xem
     * RecordingPartChangedCommandMapper.
     */
    Optional<ExamRecording> findByExamSessionIdAndStreamTypeAndSource(
        UUID examSessionId, ExamRequiredStreamType streamType, String source);

    ExamRecording save(ExamRecording recording);

    /**
     * Xoá bản ghi hình/tiếng của một phiên thi. Chỉ xoá dòng trong DB: file trên S3 để lifecycle
     * rule của bucket dọn, vì xoá file không rollback được cùng transaction.
     */
    void deleteByExamSessionId(UUID examSessionId);
}
