package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;

public interface ExamRecordingRepository {
    List<ExamRecording> findByExamSessionId(UUID examSessionId);
    Optional<ExamRecording> findByExamSessionIdAndStreamType(UUID examSessionId, ExamRequiredStreamType streamType);
    ExamRecording save(ExamRecording recording);
}
