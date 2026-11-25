package com.sep.vox.application.port.input.usecase.recording;

import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.recording.GetAssessmentRecordsQuery;
import com.sep.vox.application.port.input.service.AssessmentRecordingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.application.response.input.recording.AssessmentRecordingPlaybackResponse;
import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.repository.AssessmentRecordingRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

/**
 * The recording of an assessment session with a playback link -- for the GRADING screen, not for proctors.
 *
 * <p>Why a separate use case instead of adding a field to {@link GetAssessmentRecordsUseCase}: the
 * {@code records} query is the API of the desktop proctoring app ({@code AssessmentRecordingService}), called
 * while the session is running to track upload progress. It does not play video. Stuffing the playback link in there
 * would mean every proctor request has to sign a link for every recording -- and changing the shape of an
 * API that has a real client running, in exchange for exactly nothing.
 *
 * <p>The two paths share {@link AssessmentRecordingAccessService} so the rule "who may view" is still one.
 */
@Service
@RequiredArgsConstructor
public class GetAssessmentRecordingPlaybackUseCase
        implements IUseCase<GetAssessmentRecordsQuery, List<AssessmentRecordingPlaybackResponse>> {

    /**
     * Expiry of the playback link.
     *
     * <p>Long enough for a grader to open a long exam and seek back and forth without being cut off midway, short
     * enough that a leaked link dies early. The browser only needs the link to be alive when playback STARTS
     * and on each seek, not for the whole video duration.
     */
    private static final Duration PLAYBACK_URL_TTL = Duration.ofHours(2);

    private final AssessmentRecordingAccessService accessService;
    private final AssessmentRecordingRepository assessmentRecordingRepository;
    private final StoragePort storagePort;

    @Override
    public List<AssessmentRecordingPlaybackResponse> execute(GetAssessmentRecordsQuery input) {
        AssessmentSession session = accessService.requireCanViewRecordings(input.assessmentSessionId());
        List<AssessmentRecording> records = assessmentRecordingRepository.findByAssessmentSessionId(session.getId());

        Set<UUID> canonicalIds = canonicalRecordingIds(records);

        return records.stream()
            .map(recording -> new AssessmentRecordingPlaybackResponse(
                recording.getId(),
                recording.getStreamType(),
                recording.getStatus(),
                recording.getSource(),
                recording.getDurationSeconds(),
                canonicalIds.contains(recording.getId()),
                playbackUrlFor(recording)
            ))
            .toList();
    }

    // Keeps the proctor path way of choosing the canonical recording: each ingest source has its own row, so
    // one streamType can have several recordings. Return ALL of them and only mark the one to open first, instead
    // of filtering -- the WebRTC recording is the only one that does not pass through the candidate machine, so it
    // must stay reachable in a dispute. The grader chooses between them.
    private Set<UUID> canonicalRecordingIds(List<AssessmentRecording> records) {
        return records.stream()
            .collect(Collectors.groupingBy(
                (AssessmentRecording recording) -> recording.getStreamType(),
                Collectors.maxBy(RecordingPrecedence.CANONICAL_ORDER)))
            .values().stream()
            .flatMap(canonical -> canonical.stream())
            .map(recording -> recording.getId())
            .collect(Collectors.toSet());
    }

    /**
     * The playback link, or null when there is nothing to play yet.
     *
     * <p>Three cases return null, and all three are NORMAL, not errors: the recording is still
     * PROCESSING so it has no object key yet; the ingest source died midway (FAILED/ABANDONED); or
     * an old row was stored before {@code RECORDING_S3_BUCKET} was configured, so its bucket is empty.
     *
     * <p>Throwing here would break the whole list because of one bad recording -- while the recording of another
     * source in the same session may still play fine. Returning null lets the client show "not ready" in that
     * cell only.
     */
    private String playbackUrlFor(AssessmentRecording recording) {
        String bucket = recording.getBucket();
        String key = recording.getS3Key();
        if (bucket == null || bucket.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        return storagePort.presignRead(bucket, key, PLAYBACK_URL_TTL);
    }
}
