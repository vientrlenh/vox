package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.projection.repository.AssessmentResultAuditQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

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
public class ViewResultStatusHistoryUseCase implements IUseCase<UUID, List<ResultStatusHistoryInfo>> {

    private final AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final UserContextPort userContextPort;

    public ViewResultStatusHistoryUseCase(
            AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentRepository assessmentRepository,
            AssessmentGradingAccessService assessmentGradingAccessService,
            UserContextPort userContextPort) {
        this.assessmentResultAuditQueryRepository = assessmentResultAuditQueryRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultStatusHistoryInfo> execute(UUID candidateResultId) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var result = assessmentCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả bài thi."));

        // Chính chủ bài thi luôn xem được lịch sử của mình — kiểm trước để học sinh
        // không phải đi qua nhánh phân quyền admin và nhận ForbiddenException.
        var isOwner = assessmentCandidateRepository.findById(result.getCandidateId())
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
            // Giáo viên TỪNG được giao bài này: quyền đọc dòng thời gian đi theo việc đã
            // chấm, không theo dòng phân công còn mở. Ở vòng phúc khảo, chính người chấm
            // vòng trước là người bị hỏi lại "vì sao chấm thế" — mà lúc đó phân công của
            // họ đã COMPLETED, nên đòi phân công còn mở là khoá đúng người cần đọc nó
            // nhất. Cùng luật với nghe lại bản ghi ở AssessmentRecordingAccessService.
            //
            // Đây là quyền ĐỌC. Mọi thao tác GHI điểm vẫn đi qua
            // AssessmentGradingAccessService.authorizeAssignedTeacher trên đúng dòng đang mở.
            var wasAssignedTeacher = assessmentGradingAssignmentRepository
                .existsByCandidateResultIdAndTeacherId(candidateResultId, currentUserId);
            if (wasAssignedTeacher) {
                return assessmentResultAuditQueryRepository.findHistory(candidateResultId);
            }
            var assessment = assessmentRepository.findById(result.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
            assessmentGradingAccessService.authorizeSchoolAdmin(assessment.getSchoolId(), currentUserId);
        }
        return assessmentResultAuditQueryRepository.findHistory(candidateResultId);
    }
}
