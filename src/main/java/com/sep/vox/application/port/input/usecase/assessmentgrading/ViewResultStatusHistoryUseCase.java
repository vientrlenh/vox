package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
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
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi."));

        // Chính chủ bài thi luôn xem được lịch sử của mình — kiểm trước để học sinh
        // không phải đi qua nhánh phân quyền admin và nhận ForbiddenException.
        Boolean isOwner = assessmentCandidateRepository.findById(result.getCandidateId())
            .map(candidate -> currentUserId.equals(candidate.getStudentId()))
            .orElse(false);
        // Dòng thời gian mang scoreBefore/scoreAfter — với chính chủ nó chính là điểm, nên
        // nó chịu đúng luật công bố như màn kết quả. Trả rỗng chứ không ném lỗi: đây là
        // khối phụ trợ, ném lỗi sẽ làm hỏng cả trang chỉ vì một mục bên lề.
        if (isOwner && !AssessmentCandidateResultStatus.isVisibleToCandidate(result.getStatus())) {
            return List.of();
        }
        if (!isOwner) {
            if (userContextPort.isSystemAdmin()) {
                return assessmentResultAuditQueryRepository.findHistory(candidateResultId);
            }
            // A teacher who was EVER assigned this attempt: the permission to read the timeline follows the grading
            // that was done, not the assignment row that is still open. In the appeal round, the grader of the
            // previous round is the one being asked "why did you grade it this way" -- and by then their assignment
            // is already COMPLETED, so demanding an open assignment would lock out exactly the person who needs to read it
            // most. The same rule as listening to the recording in AssessmentRecordingAccessService.
            //
            // This is READ permission. Every score WRITE action still goes through
            // AssessmentGradingAccessService.authorizeAssignedTeacher on the row that is actually open.
            boolean wasAssignedTeacher = assessmentGradingAssignmentRepository
                .existsByCandidateResultIdAndTeacherId(candidateResultId, currentUserId);
            if (wasAssignedTeacher) {
                return assessmentResultAuditQueryRepository.findHistory(candidateResultId);
            }
            Assessment assessment = assessmentRepository.findById(result.getAssessmentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
            assessmentGradingAccessService.authorizeSchoolAdmin(assessment.getSchoolId(), currentUserId);
        }
        return assessmentResultAuditQueryRepository.findHistory(candidateResultId);
    }
}
