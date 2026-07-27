package com.sep.vox.application.port.input.usecase.recording;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RecordRecordingPartChangedCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.model.exam.ExamRecordingAssemblyStatus;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class RecordRecordingPartChangedUseCase implements IUseCase<RecordRecordingPartChangedCommand, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordRecordingPartChangedUseCase.class);

    private final ExamSessionRepository examSessionRepository;
    private final ExamRecordingRepository examRecordingRepository;
    private final String recordingBucket;

    public RecordRecordingPartChangedUseCase(
        ExamSessionRepository examSessionRepository,
        ExamRecordingRepository examRecordingRepository,
        @Value("${app.recording.bucket:}") String recordingBucket
    ) {
        this.examSessionRepository = examSessionRepository;
        this.examRecordingRepository = examRecordingRepository;
        this.recordingBucket = recordingBucket;
    }

    @Override
    public Void execute(RecordRecordingPartChangedCommand input) {
        examSessionRepository.findById(input.examSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi cho recording event: " + input.examSessionId()));

        var now = OffsetDateTime.now();
        var existing = examRecordingRepository.findByExamSessionIdAndStreamType(input.examSessionId(), input.streamType());

        if (existing.isPresent() && isTerminal(existing.get().getStatus()) && input.status() == ExamRecordingAssemblyStatus.PROCESSING) {
            LOGGER.info(
                "Skip downgrading recording examSessionId={} streamType={} from terminal status {} back to PROCESSING",
                input.examSessionId(), input.streamType(), existing.get().getStatus()
            );
            return null;
        }

        var recording = existing.orElseGet(() -> new ExamRecording(
            input.examSessionId(),
            input.candidateId(),
            input.streamType(),
            "",
            "",
            input.status(),
            null,
            null,
            now,
            null
        ));

        recording.setStatus(input.status());

        if (input.objectKey() != null && !input.objectKey().isBlank()) {
            recording.setBucket(recordingBucket);
            recording.setS3Key(input.objectKey());
        }

        if (isTerminal(input.status())) {
            if (input.durationSecs() != null) {
                recording.setDurationSeconds(Math.toIntExact(input.durationSecs()));
            }
            recording.setAssembledAt(input.occurredAt());
        }

        examRecordingRepository.save(recording);
        return null;
    }

    private boolean isTerminal(ExamRecordingAssemblyStatus status) {
        return status == ExamRecordingAssemblyStatus.READY
            || status == ExamRecordingAssemblyStatus.PARTIAL
            || status == ExamRecordingAssemblyStatus.FAILED
            || status == ExamRecordingAssemblyStatus.ABANDONED;
    }
}
