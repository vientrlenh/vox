package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;

public interface AssessmentRecordingRepository {
    List<AssessmentRecording> findByAssessmentSessionId(UUID assessmentSessionId);

    /**
     * Each ingest source keeps its own row, so source is part of the key: two ingest paths of the
     * same session no longer fight over one row. source must not be null -- see
     * RecordingPartChangedCommandMapper.
     */
    Optional<AssessmentRecording> findByAssessmentSessionIdAndStreamTypeAndSource(
        UUID assessmentSessionId, AssessmentRequiredStreamType streamType, String source);

    AssessmentRecording save(AssessmentRecording recording);

    /**
     * Deletes the video/audio recording of an assessment session. Only the DB row is deleted: the file on S3 is left to the bucket
     * lifecycle rule to clean up, because deleting a file cannot be rolled back with the transaction.
     */
    void deleteByAssessmentSessionId(UUID assessmentSessionId);
}
