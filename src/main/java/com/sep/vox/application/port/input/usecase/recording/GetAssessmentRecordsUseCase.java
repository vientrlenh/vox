package com.sep.vox.application.port.input.usecase.recording;

import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.GetAssessmentRecordsQuery;
import com.sep.vox.application.port.input.service.AssessmentRecordingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentRecordingDto;
import com.sep.vox.domain.mapper.AssessmentRecordingDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.repository.AssessmentRecordingRepository;
import com.sep.vox.domain.service.recording.RecordingPrecedence;

/**
 * The list of recordings of an assessment session -- the API of the desktop proctoring app
 * ({@code AssessmentRecordingService}), called while the session runs to track upload progress.
 *
 * <p>Does NOT return a playback link. A grading screen that needs a link should use
 * {@link GetAssessmentRecordingPlaybackUseCase} -- a separate path, so that adding the link does not make every
 * proctor request sign a URL for something it does not use.
 */
@Service
@RequiredArgsConstructor
public class GetAssessmentRecordsUseCase implements IUseCase<GetAssessmentRecordsQuery, List<AssessmentRecordingDto>> {

    private final AssessmentRecordingAccessService accessService;
    private final AssessmentRecordingRepository assessmentRecordingRepository;

    @Override
    public List<AssessmentRecordingDto> execute(GetAssessmentRecordsQuery input) {

        AssessmentSession session = accessService.requireCanViewRecordings(input.assessmentSessionId());

        AssessmentRequiredStreamType streamTypeFilter = parseStreamTypeFilter(input.streamType());

        List<AssessmentRecording> records = assessmentRecordingRepository.findByAssessmentSessionId(session.getId()).stream()
            .filter(r -> streamTypeFilter == null || r.getStreamType() == streamTypeFilter)
            .toList();

        Set<UUID> canonicalIds = records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getStreamType(),
                Collectors.maxBy(RecordingPrecedence.CANONICAL_ORDER)))
            .values().stream()
            .flatMap(o -> o.stream())
            .map(record -> record.getId())
            .collect(Collectors.toSet());

        return records.stream()
            .map(r -> AssessmentRecordingDtoMapper.toDto(r, canonicalIds.contains(r.getId())))
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
