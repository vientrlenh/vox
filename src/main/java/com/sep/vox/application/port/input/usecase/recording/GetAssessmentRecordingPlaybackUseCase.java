package com.sep.vox.application.port.input.usecase.recording;

import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
public class GetAssessmentRecordingPlaybackUseCase
        implements IUseCase<GetAssessmentRecordsQuery, List<AssessmentRecordingPlaybackResponse>> {

    /**
     * Hạn của link phát.
     *
     * <p>Đủ dài để người chấm mở một buổi thi dài rồi tua tới lui mà không bị đứt giữa chừng, đủ
     * ngắn để link lỡ lọt ra ngoài thì cũng chết sớm. Trình duyệt chỉ cần link còn sống lúc BẮT
     * ĐẦU phát và mỗi lần tua, không phải suốt thời lượng video.
     */
    private static final Duration PLAYBACK_URL_TTL = Duration.ofHours(2);

    private final AssessmentRecordingAccessService accessService;
    private final AssessmentRecordingRepository assessmentRecordingRepository;
    private final StoragePort storagePort;

    public GetAssessmentRecordingPlaybackUseCase(
            AssessmentRecordingAccessService accessService,
            AssessmentRecordingRepository assessmentRecordingRepository,
            StoragePort storagePort) {
        this.accessService = accessService;
        this.assessmentRecordingRepository = assessmentRecordingRepository;
        this.storagePort = storagePort;
    }

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
