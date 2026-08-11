package com.sep.vox.application.port.input.usecase.recording;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.GetExamRecordsQuery;
import com.sep.vox.application.port.input.service.ExamRecordingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamRecordingDto;
import com.sep.vox.domain.mapper.ExamRecordingDtoMapper;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

/**
 * Danh sách bản ghi của một phiên thi -- API của app giám thị trên desktop
 * ({@code ExamRecordingService}), gọi trong lúc ca thi chạy để theo dõi tiến độ tải lên.
 *
 * <p>KHÔNG trả link phát. Màn chấm bài cần link thì dùng
 * {@link GetExamRecordingPlaybackUseCase} -- đường riêng, để việc thêm link không bắt mọi lượt
 * hỏi của giám thị phải ký URL cho một thứ nó không dùng.
 */
@Service
public class GetExamRecordsUseCase implements IUseCase<GetExamRecordsQuery, List<ExamRecordingDto>> {

    private final ExamRecordingAccessService accessService;
    private final ExamRecordingRepository examRecordingRepository;

    public GetExamRecordsUseCase(
        ExamRecordingAccessService accessService,
        ExamRecordingRepository examRecordingRepository
    ) {
        this.accessService = accessService;
        this.examRecordingRepository = examRecordingRepository;
    }

    @Override
    public List<ExamRecordingDto> execute(GetExamRecordsQuery input) {

        var session = accessService.requireCanViewRecordings(input.examSessionId());

        var streamTypeFilter = parseStreamTypeFilter(input.streamType());

        var records = examRecordingRepository.findByExamSessionId(session.getId()).stream()
            .filter(r -> streamTypeFilter == null || r.getStreamType() == streamTypeFilter)
            .toList();

        var canonicalIds = records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getStreamType(),
                Collectors.maxBy(RecordingPrecedence.CANONICAL_ORDER)))
            .values().stream()
            .flatMap(o -> o.stream())
            .map(record -> record.getId())
            .collect(Collectors.toSet());

        return records.stream()
            .map(r -> ExamRecordingDtoMapper.toDto(r, canonicalIds.contains(r.getId())))
            .toList();
    }
    
    private ExamRequiredStreamType parseStreamTypeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        ExamRequiredStreamType type;
        try {
            type = ExamRequiredStreamType.valueOf(filter.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại stream không hợp lệ: " + filter);
        }
        if (type ==  ExamRequiredStreamType.CAMERA_AND_SCREEN) {
            throw new IllegalArgumentException("Loại stream yêu cầu cho bản ghi chỉ hỗ trợ màn hình hoặc camera");
        }
        return type;
    }
}
