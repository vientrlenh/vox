package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Dòng thời gian điểm của một bài: ai, lúc nào, từ trạng thái nào sang trạng thái nào,
 * vì sao, điểm trước → sau.
 *
 * <p>Ba nhóm người xem, ba lý do khác nhau: school admin cần nó để giải trình tranh
 * chấp, học sinh cần nó để hiểu vì sao điểm mình thay đổi, và giáo viên ĐÃ TỪNG được
 * giao bài cần nó làm ngữ cảnh — nhất là ở vòng phúc khảo, nơi câu hỏi đúng là "điểm
 * này đã đi qua những tay nào". Học sinh chỉ xem được bài của chính mình; giáo viên chỉ
 * xem được bài mình từng cầm, kể cả khi phân công đó đã đóng.
 */
@Service
@RequiredArgsConstructor
public class ViewResultStatusHistoryUseCase implements IUseCase<UUID, List<ResultStatusHistoryInfo>> {

    private final AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<ResultStatusHistoryInfo> execute(UUID candidateResultId) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        AssessmentCandidateResult result = assessmentCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả bài thi."));

        // The owner of the attempt can always see their own history -- checked first so a student does not
        // go through the admin authorization branch and receive ForbiddenException.
        boolean isOwner = assessmentCandidateRepository.findById(result.getCandidateId())
            .map(candidate -> currentUserId.equals(candidate.getStudentId()))
            .orElse(false);
        // The timeline carries scoreBefore/scoreAfter -- for the owner that IS the score, so it follows the same
        // publication rule as the result screen. Returns empty instead of throwing: this is an auxiliary block,
        // throwing would break the whole page just because of a side item.
        if (isOwner && !AssessmentCandidateResultStatus.isVisibleToCandidate(result.getStatus())) {
            return List.of();
        }
        if (!isOwner) {
            requireHistoryReadAccess(result, candidateResultId, currentUserId);
        }
        return assessmentResultAuditQueryRepository.findHistory(candidateResultId);
    }

    private void requireHistoryReadAccess(AssessmentCandidateResult result, UUID candidateResultId, UUID currentUserId) {
        if (userContextPort.isSystemAdmin()) {
            return;
        }
        // A teacher who was EVER assigned this attempt: the permission to read the timeline follows the grading
        // that was done, not the assignment row that is still open. In the appeal round, the grader of the
        // previous round is the one being asked "why did you grade it this way" -- and by then their assignment
        // is already COMPLETED, so demanding an open assignment would lock out exactly the person who needs to read it
        // most. The same rule as listening to the recording in AssessmentRecordingAccessService.
        //
        // This is READ permission. Every score WRITE action still goes through
        // AssessmentGradingAccessService.authorizeAssignedTeacher on the row that is actually open.
        if (assessmentGradingAssignmentRepository.existsByCandidateResultIdAndTeacherId(candidateResultId, currentUserId)) {
            return;
        }
        Assessment assessment = assessmentRepository.findById(result.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
        assessmentGradingAccessService.authorizeSchoolAdmin(assessment.getSchoolId(), currentUserId);
    }
}
