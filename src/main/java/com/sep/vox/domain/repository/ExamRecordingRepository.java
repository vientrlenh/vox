package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

public interface ExamRecordingRepository {
    List<ExamRecordingEntry> findByStudentIdWithAudio(UUID studentId);
}
