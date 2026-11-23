package com.sep.vox.application.port.input.usecase.recording;

import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.GetAssessmentRecordsQuery;
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

        // Giữ nguyên cách chọn bản chuẩn của đường giám thị: mỗi nguồn ingest có hàng riêng nên
        // một streamType có thể có nhiều bản. Trả HẾT và chỉ đánh dấu bản nên mở trước, thay vì
        // lọc bớt -- bản WebRTC là bản duy nhất không đi qua máy thí sinh, nên khi có tranh chấp
        // nó phải với tới được. Người chấm tự chọn giữa chúng.
        Set<UUID> canonicalIds = records.stream()
            .collect(Collectors.groupingBy(
                (AssessmentRecording recording) -> recording.getStreamType(),
                Collectors.maxBy(RecordingPrecedence.CANONICAL_ORDER)))
            .values().stream()
            .flatMap(canonical -> canonical.stream())
            .map(recording -> recording.getId())
            .collect(Collectors.toSet());

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

    /**
     * Link phát, hoặc null khi chưa có gì để phát.
     *
     * <p>Ba trường hợp trả null, cả ba đều BÌNH THƯỜNG chứ không phải lỗi: bản ghi đang
     * PROCESSING nên chưa có object key; nguồn ingest chết giữa chừng (FAILED/ABANDONED); hoặc
     * hàng cũ lưu từ trước khi {@code RECORDING_S3_BUCKET} được cấu hình nên bucket rỗng.
     *
     * <p>Ném ở đây sẽ làm hỏng cả danh sách vì một bản ghi hỏng -- trong khi bản của nguồn khác
     * cùng phiên thi có thể vẫn xem tốt. Trả null để client hiện "chưa sẵn sàng" đúng ô đó.
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
