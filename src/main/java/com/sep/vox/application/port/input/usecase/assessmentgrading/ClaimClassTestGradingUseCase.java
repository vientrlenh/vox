package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.ClaimClassTestGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.service.assessment.GradingRoundPolicy;

/**
 * Giáo viên tạo bài kiểm tra trên lớp tự nhận chấm — không qua nhà trường.
 *
 * <p>Đặt ở facade class test thay vì nới {@code @PreAuthorize} của
 * {@code GradingAssignmentController}: nới chỗ đó là mở cửa cho <em>mọi</em> giáo viên
 * trên <em>mọi</em> kỳ thi, còn ở đây phạm vi đóng đúng bằng bài mà người gọi làm CHAIR.
 *
 * <p>Vòng {@code APPEAL} KHÔNG nhận qua đây: nó gắn với một đơn phúc khảo cụ thể nên
 * phải đi qua {@code AssignAssessmentAppealReviewerUseCase} — chỗ duy nhất biết luật xung
 * đột lợi ích và biết cách chuyển trạng thái đơn.
 *
 * <p>Validate hết rồi mới ghi: một dòng sai không được để lại phân công nửa vời của
 * các dòng trước.
 */
@Service
public class ClaimClassTestGradingUseCase
        implements IUseCase<ClaimClassTestGradingCommand, List<UUID>> {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ClaimClassTestGradingUseCase(
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional
    public List<UUID> execute(ClaimClassTestGradingCommand command) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        assessmentGradingAccessService.authorizeClassTestChair(command.assessmentId(), currentUserId);

        var roundType = parseRoundType(command.roundType());
        var candidateResultIds = command.candidateResultIds() == null
            ? List.<UUID>of() : command.candidateResultIds();
        if (candidateResultIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một bài để nhận chấm.");
        }
        if (new HashSet<>(candidateResultIds).size() != candidateResultIds.size()) {
            throw new ResourceDuplicatedException("Không được chọn trùng bài.");
        }

        var resultsById = assessmentCandidateResultRepository.findByIdIn(candidateResultIds).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));

        // Một query cho cả lô — pre-check để báo lỗi tiếng Việt thay vì để unique index
        // ném DataIntegrityViolation; index vẫn là chốt cuối khi có race.
        var alreadyOpen = assessmentGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(candidateResultIds).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());

        var now = Instant.now();
        var assignments = new java.util.ArrayList<AssessmentGradingAssignment>();
        for (var candidateResultId : candidateResultIds) {
            var result = resultsById.get(candidateResultId);
            if (result == null) {
                throw new ResourceNotFoundException("Không tìm thấy kết quả bài thi.");
            }
            // Chặn nhận bài của bài kiểm tra khác — assessmentId ở URL mới là thứ đã qua phân quyền.
            if (!command.assessmentId().equals(result.getAssessmentId())) {
                throw new ResourceNotFoundException("Bài thi không thuộc bài kiểm tra này.");
            }
            if (!GradingRoundPolicy.isAssignable(roundType, result.getStatus())) {
                throw new IllegalStateException(
                    "Bài thi không ở trạng thái phù hợp với vòng chấm " + roundType.name() + ".");
            }
            if (alreadyOpen.contains(candidateResultId)) {
                throw new ResourceDuplicatedException("Bài thi này đang được chấm.");
            }

            // scoreBefore chụp NGAY LÚC NHẬN — mốc đo độ lệch so với vòng trước.
            assignments.add(AssessmentGradingAssignment.open(
                candidateResultId,
                currentUserId,
                roundType,
                null,
                result.getTotalScore(),
                now,
                currentUserId,
                null
            ));
        }

        return assessmentGradingAssignmentRepository.saveAll(assignments).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private AssessmentGradingRoundType parseRoundType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phải chọn vòng chấm.");
        }
        AssessmentGradingRoundType roundType;
        try {
            roundType = AssessmentGradingRoundType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Vòng chấm không hợp lệ.");
        }
        if (roundType == AssessmentGradingRoundType.APPEAL) {
            throw new IllegalArgumentException(
                "Vòng phúc khảo được nhận từ màn đơn phúc khảo, không nhận ở đây.");
        }
        return roundType;
    }
}
