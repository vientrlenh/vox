package com.sep.vox.application.port.input.usecase.recording;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.GetExamRecordsQuery;
import com.sep.vox.application.port.input.service.ExamRecordingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.application.response.input.recording.ExamRecordingPlaybackResponse;
import com.sep.vox.domain.model.exam.ExamRecording;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

/**
 * Bản ghi buổi thi kèm link phát -- dành cho màn CHẤM BÀI, không phải cho giám thị.
 *
 * <p>Vì sao là use case riêng chứ không thêm field vào {@link GetExamRecordsUseCase}: query
 * {@code records} là API của app giám thị trên desktop ({@code ExamRecordingService}), gọi trong
 * lúc ca thi đang chạy để theo dõi tiến độ tải lên. Nó không phát video. Nhét link phát vào đó
 * nghĩa là mỗi lượt hỏi của giám thị đều phải ký link cho mọi bản ghi -- và đổi hình dạng một
 * API đang có client chạy thật, để đánh đổi lấy đúng con số không.
 *
 * <p>Hai đường dùng chung {@link ExamRecordingAccessService} nên luật "ai được xem" vẫn là một.
 */
@Service
public class GetExamRecordingPlaybackUseCase
        implements IUseCase<GetExamRecordsQuery, List<ExamRecordingPlaybackResponse>> {

    /**
     * Hạn của link phát.
     *
     * <p>Đủ dài để người chấm mở một buổi thi dài rồi tua tới lui mà không bị đứt giữa chừng, đủ
     * ngắn để link lỡ lọt ra ngoài thì cũng chết sớm. Trình duyệt chỉ cần link còn sống lúc BẮT
     * ĐẦU phát và mỗi lần tua, không phải suốt thời lượng video.
     */
    private static final Duration PLAYBACK_URL_TTL = Duration.ofHours(2);

    private final ExamRecordingAccessService accessService;
    private final ExamRecordingRepository examRecordingRepository;
    private final StoragePort storagePort;

    public GetExamRecordingPlaybackUseCase(
            ExamRecordingAccessService accessService,
            ExamRecordingRepository examRecordingRepository,
            StoragePort storagePort) {
        this.accessService = accessService;
        this.examRecordingRepository = examRecordingRepository;
        this.storagePort = storagePort;
    }

    @Override
    public List<ExamRecordingPlaybackResponse> execute(GetExamRecordsQuery input) {
        var session = accessService.requireCanViewRecordings(input.examSessionId());
        var records = examRecordingRepository.findByExamSessionId(session.getId());

        // Giữ nguyên cách chọn bản chuẩn của đường giám thị: mỗi nguồn ingest có hàng riêng nên
        // một streamType có thể có nhiều bản. Trả HẾT và chỉ đánh dấu bản nên mở trước, thay vì
        // lọc bớt -- bản WebRTC là bản duy nhất không đi qua máy thí sinh, nên khi có tranh chấp
        // nó phải với tới được. Người chấm tự chọn giữa chúng.
        var canonicalIds = records.stream()
            .collect(Collectors.groupingBy(
                ExamRecording::getStreamType,
                Collectors.maxBy(RecordingPrecedence.CANONICAL_ORDER)))
            .values().stream()
            .flatMap(java.util.Optional::stream)
            .map(ExamRecording::getId)
            .collect(Collectors.toSet());

        return records.stream()
            .map(recording -> new ExamRecordingPlaybackResponse(
                recording.getId(),
                recording.getStreamType().name(),
                recording.getStatus().name(),
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
    private String playbackUrlFor(ExamRecording recording) {
        var bucket = recording.getBucket();
        var key = recording.getS3Key();
        if (bucket == null || bucket.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        return storagePort.presignRead(bucket, key, PLAYBACK_URL_TTL);
    }
}
