package com.sep.vox.application.port.input.usecase.recording;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RecordRecordingPartChangedCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

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

        var now = Instant.now();
        // Tra cứu kèm source: mỗi nguồn ingest chỉ cập nhật hàng của chính nó, nên hai đường
        // ingest của cùng một phiên thi cùng tồn tại thay vì đường về sau ghi đè đường về trước.
        // Việc chọn bản nào là bản chuẩn diễn ra ở đường đọc (GetExamRecordsUseCase).
        var existing = examRecordingRepository.findByExamSessionIdAndStreamTypeAndSource(
            input.examSessionId(), input.streamType(), input.source());

        if (existing.isPresent()
            && RecordingPrecedence.shouldKeepExisting(existing.get().getStatus(), input.status())) {
            LOGGER.info(
                "Giữ bản ghi hiện có examSessionId={} streamType={} source={} status={}; bỏ qua event status={} objectKey={}",
                input.examSessionId(), input.streamType(),
                existing.get().getSource(), existing.get().getStatus(),
                input.status(), input.objectKey()
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

        examRecordingRepository.save(recording);
        return null;
    }

}
