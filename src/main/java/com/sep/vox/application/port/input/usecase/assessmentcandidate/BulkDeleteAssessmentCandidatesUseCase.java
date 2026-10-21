package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.BulkDeleteAssessmentCandidatesCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

/**
 * Xoá cả nhóm thí sinh khỏi kỳ thi trong đúng MỘT transaction.
 *
 * <p>Cùng luật với {@link DeleteAssessmentCandidateUseCase} cho từng người, chỉ khác ở chỗ soát cả lô
 * bằng hai query rồi mới đụng vào dữ liệu. Để giao diện gọi endpoint xoá từng người N lần thì xoá
 * 40 thí sinh mà hỏng ở người thứ 25 sẽ để lại danh sách dở dang — giống lý do
 * {@code BulkAssignAssessmentCandidateScheduleUseCase} tồn tại.
 *
 * <p>All-or-nothing: chỉ cần MỘT thí sinh trong lô đã có bài thi là cả lượt bị từ chối, kèm tên
 * đích danh những người vướng. Xoá một phần rồi báo lỗi chung chung sẽ khiến người dùng không biết
 * ai đã bị xoá và ai chưa, mà đây là thao tác không hoàn tác được.
 */
@Service
public class BulkDeleteAssessmentCandidatesUseCase implements IUseCase<BulkDeleteAssessmentCandidatesCommand, Void> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public BulkDeleteAssessmentCandidatesUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(BulkDeleteAssessmentCandidatesCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        authorize(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        var candidateIds = input.candidateIds() == null ? List.<UUID>of() : input.candidateIds();
        if (candidateIds.isEmpty()) {
            return null;
        }

        var candidates = assessmentCandidateRepository.findByIdInAndAssessmentId(candidateIds, assessment.getId());
        // Tra theo (id, assessmentId) nên thiếu dòng nghĩa là có id không tồn tại hoặc thuộc kỳ thi khác —
        // hỏng cả lượt thay vì âm thầm xoá một phần.
        if (candidates.size() != candidateIds.stream().distinct().count()) {
            throw new ResourceNotFoundException("Không tìm thấy thí sinh");
        }

        // Một query cho cả lô: phiên đã xoá mềm không tính (findByCandidateIdIn đã loại DELETED),
        // nên thí sinh chỉ còn lượt thi đã xoá vẫn gỡ được khỏi kỳ thi.
        var busyCandidateIds = assessmentSessionRepository
            .findByCandidateIdIn(candidates.stream().map(candidate -> candidate.getId()).toList())
            .stream()
            .map(session -> session.getCandidateId())
            .collect(java.util.stream.Collectors.toSet());

        if (!busyCandidateIds.isEmpty()) {
            // Chỉ nêu SỐ LƯỢNG: ở tầng này chỉ có studentId dạng UUID, in ra thì người dùng cũng
            // không nhận ra ai. Giao diện đã loại sẵn nhóm này khỏi lượt xoá nên đây là lưới an
            // toàn cho trường hợp danh sách vừa đổi, và người dùng chỉ cần biết phải chọn lại.
            throw new IllegalStateException(busyCandidateIds.size()
                + " thí sinh trong danh sách đã có bài thi nên không thể xóa khỏi kỳ thi."
                + " Hãy tải lại danh sách và chọn lại.");
        }

        assessmentCandidateRepository.deleteByIdIn(candidateIds);
        return null;
    }

    /** Cùng luật với {@link DeleteAssessmentCandidateUseCase}: quản trị trường sở tại hoặc chủ tịch hội đồng. */
    private void authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
