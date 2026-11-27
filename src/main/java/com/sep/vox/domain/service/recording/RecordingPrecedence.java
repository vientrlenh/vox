package com.sep.vox.domain.service.recording;

import java.util.Comparator;
import java.util.Map;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;

/**
 * Decides which recording is considered canonical when an assessment session has several of the same streamType.
 *
 * <p>The two ingest paths mint their streamID independently so they produce two recording.mp4 files under two different S3
 * prefixes. Each source keeps its own row in assessment_recordings, so no source overwrites another:
 * choosing which one to open first is deferred to read time, and none is thrown away.
 */
public final class RecordingPrecedence {

    private RecordingPrecedence() {
    }

    /**
     * Which source is more trustworthy when an assessment session has several recordings. A higher rank is chosen as
     * the canonical recording.
     *
     * <p>SERVER_WATCHDOG ranks last because it is the only case where nobody confirmed the stream ended cleanly:
     * it stitches together what was received from a client that went silent, so completeness is undetermined.
     */
    private static final Map<String, Integer> SOURCE_RANK = Map.of(
        "DESKTOP_SEGMENT_UPLOAD", 3,
        "SERVER_WEBRTC", 2,
        "SERVER_WATCHDOG", 1
    );

    /**
     * An unknown or null source ranks below every known source. Rows written before the source column existed
     * fall into this group: they still show up and can still be opened, they are just not preferred as the canonical
     * recording once there is one whose origin is known.
     */
    public static int rankOf(String source) {
        return source == null ? 0 : SOURCE_RANK.getOrDefault(source, 0);
    }

    /** Statuses in which the record really points at a file that can be opened. */
    public static boolean hasRecording(AssessmentRecordingAssemblyStatus status) {
        return status == AssessmentRecordingAssemblyStatus.READY
            || status == AssessmentRecordingAssemblyStatus.PARTIAL;
    }

    public static boolean isTerminal(AssessmentRecordingAssemblyStatus status) {
        return status == AssessmentRecordingAssemblyStatus.READY
            || status == AssessmentRecordingAssemblyStatus.PARTIAL
            || status == AssessmentRecordingAssemblyStatus.FAILED
            || status == AssessmentRecordingAssemblyStatus.ABANDONED;
    }

    /**
     * The order for choosing the canonical recording: one with an openable file first, then the reliability of the source. The
     * largest in this order is the canonical recording of the group.
     *
     * <p>The first clause must come before the second: a FAILED from a high-priority source must not become the canonical one
     * while a READY from a lower source still exists. A broken label does not beat a viewable file.
     */
    public static final Comparator<AssessmentRecording> CANONICAL_ORDER =
        Comparator.<AssessmentRecording, Boolean>comparing(recording -> hasRecording(recording.getStatus()))
            .thenComparingInt(recording -> rankOf(recording.getSource()));

    /**
     * Whether to keep the existing row and ignore the event that just arrived.
     *
     * <p>Now only a rule within a single row: since source became part of the lookup key, both sides
     * always belong to the same source, so there is no more case of one source overwriting another. The comparison
     * across sources moved to {@link #CANONICAL_ORDER} on the read path.
     */
    public static boolean shouldKeepExisting(
        AssessmentRecordingAssemblyStatus existingStatus,
        AssessmentRecordingAssemblyStatus incomingStatus
    ) {
        if (!isTerminal(existingStatus)) {
            return false;
        }
        // Do not downgrade a final result back to "processing".
        if (incomingStatus == AssessmentRecordingAssemblyStatus.PROCESSING) {
            return true;
        }
        // A failure must not erase a usable record with its own broken label.
        return hasRecording(existingStatus) && !hasRecording(incomingStatus);
    }
}
