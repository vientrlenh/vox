package com.sep.vox.application.port.input.usecase.recording;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.RecordRecordingPartChangedCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.repository.assessment.AssessmentRecordingRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

@Service
public class RecordRecordingPartChangedUseCase implements IUseCase<RecordRecordingPartChangedCommand, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordRecordingPartChangedUseCase.class);

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRecordingRepository assessmentRecordingRepository;
    private final String recordingBucket;

    public RecordRecordingPartChangedUseCase(
        AssessmentSessionRepository assessmentSessionRepository,
        AssessmentRecordingRepository assessmentRecordingRepository,
        @Value("${app.recording.bucket:}") String recordingBucket
    ) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentRecordingRepository = assessmentRecordingRepository;
        this.recordingBucket = recordingBucket;
    }

    @Override
    public Void execute(RecordRecordingPartChangedCommand input) {
        assessmentSessionRepository.findById(input.assessmentSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi cho recording event: " + input.assessmentSessionId()));

        var now = Instant.now();
        // Tra cứu kèm source: mỗi nguồn ingest chỉ cập nhật hàng của chính nó, nên hai đường
        // ingest của cùng một phiên thi cùng tồn tại thay vì đường về sau ghi đè đường về trước.
        // Việc chọn bản nào là bản chuẩn diễn ra ở đường đọc (GetAssessmentRecordsUseCase).
        var existing = assessmentRecordingRepository.findByAssessmentSessionIdAndStreamTypeAndSource(
            input.assessmentSessionId(), input.streamType(), input.source());

        if (existing.isPresent()
            && RecordingPrecedence.shouldKeepExisting(existing.get().getStatus(), input.status())) {
            LOGGER.info(
                "Giữ bản ghi hiện có assessmentSessionId={} streamType={} source={} status={}; bỏ qua event status={} objectKey={}",
                input.assessmentSessionId(), input.streamType(),
                existing.get().getSource(), existing.get().getStatus(),
                input.status(), input.objectKey()
            );
            return null;
        }

        var recording = existing.orElseGet(() -> new AssessmentRecording(
            input.assessmentSessionId(),
            input.candidateId(),
            input.streamType(),
            "",
            "",
            input.status(),
            null,
            null,
            input.source(),
            now,
            null
        ));

        // Không set lại source: nó là một phần danh tính của hàng, nên hàng tìm được luôn đã
        // thuộc đúng nguồn này rồi.
        recording.setStatus(input.status());

        if (input.objectKey() != null && !input.objectKey().isBlank()) {
            recording.setBucket(recordingBucket);
            recording.setS3Key(input.objectKey());
        }

        if (RecordingPrecedence.isTerminal(input.status())) {
            if (input.durationSecs() != null) {
                recording.setDurationSeconds(Math.toIntExact(input.durationSecs()));
            }
            recording.setAssembledAt(input.occurredAt());
        }

        assessmentRecordingRepository.save(recording);
        return null;
    }

}
