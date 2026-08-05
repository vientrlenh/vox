package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamResultVisibilityPolicy;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.ExamResultAuditQueryRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Dòng thời gian điểm của một bài: ai, lúc nào, từ trạng thái nào sang trạng thái nào,
 * vì sao, điểm trước → sau.
 *
 * <p>Ba nhóm người xem, ba lý do khác nhau: school admin cần nó để giải trình tranh
 * chấp, học sinh cần nó để hiểu vì sao điểm mình thay đổi, và giáo viên ĐANG được giao
 * bài cần nó làm ngữ cảnh khi chấm — nhất là ở vòng phúc khảo, nơi câu hỏi đúng là
 * "điểm này đã đi qua những tay nào". Học sinh chỉ xem được bài của chính mình; giáo
 * viên chỉ xem được bài mình đang cầm, hết phân công là hết quyền.
 */
@Service
public class ViewResultStatusHistoryUseCase implements IUseCase<UUID, List<ResultStatusHistoryInfo>> {

    private final ExamResultAuditQueryRepository examResultAuditQueryRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamRepository examRepository;
    private final ExamGradingAccessService examGradingAccessService;
    private final UserContextPort userContextPort;

    public ViewResultStatusHistoryUseCase(
            ExamResultAuditQueryRepository examResultAuditQueryRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamRepository examRepository,
            ExamGradingAccessService examGradingAccessService,
            UserContextPort userContextPort) {
        this.examResultAuditQueryRepository = examResultAuditQueryRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examRepository = examRepository;
        this.examGradingAccessService = examGradingAccessService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultStatusHistoryInfo> execute(UUID candidateResultId) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var result = examCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi."));

        // Chính chủ bài thi luôn xem được lịch sử của mình — kiểm trước để học sinh
        // không phải đi qua nhánh phân quyền admin và nhận ForbiddenException.
        var isOwner = examCandidateRepository.findById(result.getCandidateId())
            .map(candidate -> currentUserId.equals(candidate.getStudentId()))
            .orElse(false);
        // Dòng thời gian mang scoreBefore/scoreAfter — với chính chủ nó chính là điểm, nên
        // nó chịu đúng luật công bố như màn kết quả. Trả rỗng chứ không ném lỗi: đây là
        // khối phụ trợ, ném lỗi sẽ làm hỏng cả trang chỉ vì một mục bên lề.
        if (isOwner && !ExamResultVisibilityPolicy.isVisibleToCandidate(result.getStatus())) {
            return List.of();
        }
        if (!isOwner) {
            if (userContextPort.isSystemAdmin()) {
                return examResultAuditQueryRepository.findHistory(candidateResultId);
            }
            // Giáo viên đang cầm bài: quyền đến từ chính dòng phân công đang mở, đúng
            // khuôn với mọi thao tác chấm khác. Phân công đóng lại là quyền mất theo.
            var isAssignedTeacher = examGradingAssignmentRepository
                .findOpenByCandidateResultId(candidateResultId)
                .map(assignment -> currentUserId.equals(assignment.getTeacherId()))
                .orElse(false);
            if (isAssignedTeacher) {
                return examResultAuditQueryRepository.findHistory(candidateResultId);
            }
            var exam = examRepository.findById(result.getExamId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
            examGradingAccessService.authorizeSchoolAdmin(exam.getSchoolId(), currentUserId);
        }
        return examResultAuditQueryRepository.findHistory(candidateResultId);
    }
}
