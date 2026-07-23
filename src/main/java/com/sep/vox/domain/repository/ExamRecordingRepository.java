package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamRecording;

public interface ExamRecordingRepository {
    List<ExamRecording> findByExamSessionId(UUID examSessionId);
}
