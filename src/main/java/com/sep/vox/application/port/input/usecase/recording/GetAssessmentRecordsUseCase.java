package com.sep.vox.application.port.input.usecase.recording;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.GetAssessmentRecordsQuery;
import com.sep.vox.application.port.input.service.AssessmentRecordingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.assessment.AssessmentRecordingDto;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.repository.assessment.AssessmentRecordingRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

/**
 * Danh sách bản ghi của một phiên thi -- API của app giám thị trên desktop
 * ({@code AssessmentRecordingService}), gọi trong lúc ca thi chạy để theo dõi tiến độ tải lên.
 *
 * <p>KHÔNG trả link phát. Màn chấm bài cần link thì dùng
 * {@link GetAssessmentRecordingPlaybackUseCase} -- đường riêng, để việc thêm link không bắt mọi lượt
 * hỏi của giám thị phải ký URL cho một thứ nó không dùng.
 */
@Service
public class GetAssessmentRecordsUseCase implements IUseCase<GetAssessmentRecordsQuery, List<AssessmentRecordingDto>> {

    private final AssessmentRecordingAccessService accessService;
    private final AssessmentRecordingRepository assessmentRecordingRepository;

    public GetAssessmentRecordsUseCase(
        AssessmentRecordingAccessService accessService,
        AssessmentRecordingRepository assessmentRecordingRepository
    ) {
        this.accessService = accessService;
        this.assessmentRecordingRepository = assessmentRecordingRepository;
    }

    @Override
    public List<AssessmentRecordingDto> execute(GetAssessmentRecordsQuery input) {

        var session = accessService.requireCanViewRecordings(input.assessmentSessionId());

        var streamTypeFilter = parseStreamTypeFilter(input.streamType());

        var records = assessmentRecordingRepository.findByAssessmentSessionId(session.getId()).stream()
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
            .map(r -> AssessmentRecordingDto.toDto(r, canonicalIds.contains(r.getId())))
            .toList();
    }
    
    private AssessmentRequiredStreamType parseStreamTypeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        AssessmentRequiredStreamType type;
        try {
            type = AssessmentRequiredStreamType.valueOf(filter.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại stream không hợp lệ: " + filter);
        }
        if (type ==  AssessmentRequiredStreamType.CAMERA_AND_SCREEN) {
            throw new IllegalArgumentException("Loại stream yêu cầu cho bản ghi chỉ hỗ trợ màn hình hoặc camera");
        }
        return type;
    }
}
