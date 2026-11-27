package com.sep.vox.application.port.input.usecase.recording;

import java.time.Instant;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.recording.RecordRecordingPartChangedCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.RecordingStorageConfigPort;
import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.repository.AssessmentRecordingRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordRecordingPartChangedUseCase implements IUseCase<RecordRecordingPartChangedCommand, Void> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRecordingRepository assessmentRecordingRepository;
    private final RecordingStorageConfigPort recordingStorageConfigPort;

    @Override
    public Void execute(RecordRecordingPartChangedCommand input) {
        assessmentSessionRepository.findById(input.assessmentSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found for the recording event: " + input.assessmentSessionId()));

        Instant now = Instant.now();
        // Lookup with the source included: each ingest source only updates its own row, so the two ingest
        // paths of the same assessment session coexist instead of the later one overwriting the earlier one.
        // Choosing which one is canonical happens on the read path (GetAssessmentRecordsUseCase).
        Optional<AssessmentRecording> existing = assessmentRecordingRepository.findByAssessmentSessionIdAndStreamTypeAndSource(
            input.assessmentSessionId(), input.streamType(), input.source());

        if (existing.isPresent()
            && RecordingPrecedence.shouldKeepExisting(existing.get().getStatus(), input.status())) {
            logKeptExistingRecording(existing.get(), input);
            return null;
        }

        AssessmentRecording recording = existing.orElseGet(() -> AssessmentRecording.registered(
                input.assessmentSessionId(), input.candidateId(), input.streamType(), input.source(), input.status(), now));

        applyPartChange(recording, input);

        assessmentRecordingRepository.save(recording);
        return null;
    }

    private void logKeptExistingRecording(AssessmentRecording existing, RecordRecordingPartChangedCommand input) {
        log.info(
            "Keeping the existing recording assessmentSessionId={} streamType={} source={} status={}; ignoring event status={} objectKey={}",
            input.assessmentSessionId(), input.streamType(),
            existing.getSource(), existing.getStatus(),
            input.status(), input.objectKey()
        );
    }

    private void applyPartChange(AssessmentRecording recording, RecordRecordingPartChangedCommand input) {
        // The source is not set again: it is part of the row identity, so the row found already belongs to
        // this source.
        recording.setStatus(input.status());

        if (input.objectKey() != null && !input.objectKey().isBlank()) {
            recording.setBucket(recordingStorageConfigPort.bucket());
            recording.setS3Key(input.objectKey());
        }

        if (RecordingPrecedence.isTerminal(input.status())) {
            if (input.durationSecs() != null) {
                recording.setDurationSeconds(Math.toIntExact(input.durationSecs()));
            }
            recording.setAssembledAt(input.occurredAt());
        }
    }

}
