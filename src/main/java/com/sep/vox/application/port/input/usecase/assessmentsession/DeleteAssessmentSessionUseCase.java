package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

/**
 * Xoá MỀM một phiên thi: đánh dấu phiên (và kết quả của phiên đó) là {@code DELETED} kèm thời điểm
 * và lý do — dùng để gỡ một lượt thi hỏng vì vào phòng lỗi hoặc chấm lỗi.
 *
 * <p>Bản trước xoá CỨNG cả cây dữ liệu (câu trả lời, lượt nói, đánh giá, điểm theo tiêu chí, kết
 * quả, phúc khảo, phân công chấm, nhật ký trạng thái, bản ghi hình). Ba vấn đề khiến nó bị thay:
 *
 * <ul>
 *   <li>Không hoàn tác được và không kiểm chứng lại được. Xoá nhầm một bài thi là mất trắng bằng
 *       chứng, đúng lúc cần nhất là khi học sinh thắc mắc điểm.
 *   <li>Cascade viết tay qua hơn mười bảng, không có FK nào đỡ: sót một bảng là để lại dòng mồ côi.
 *   <li>Hai khoá ngoại trỏ vào {@code assessment_sessions} mà cascade đó không hề dọn
 *       ({@code school_balance_entries.assessment_session_id}, {@code school_debt_events.trigger_assessment_session_id}),
 *       cả hai đều {@code NO ACTION} — xoá một phiên đã phát sinh chi phí AI là vi phạm khoá ngoại.
 *       Giữ dòng lại thì cả hai tham chiếu vẫn hợp lệ.
 * </ul>
 *
 * <p>Dữ liệu bài làm được giữ NGUYÊN, chỉ ẩn khỏi các luồng đọc. Quản trị trường và chủ tịch hội
 * đồng vẫn thấy được phiên đã xoá (có nhãn "Đã xoá" kèm lý do) — xem {@code AssessmentSessionJpaEntity}.
 *
 * <p>Phân quyền: SCHOOL_ADMIN xoá được mọi phiên thuộc trường mình; chủ tịch xoá được phiên của kỳ
 * thi mình làm chủ tịch, KHÔNG phân biệt loại kỳ thi — khớp với
 * {@code UpdateAssessmentUseCase.authorizeMutation}, nơi chủ tịch kỳ thi tập trung sửa được kỳ thi như
 * quản trị trường.
 *
 * <p>Trước đây nhánh chủ tịch còn đòi thêm {@code CLASS_TEST} và viện dẫn chính
 * {@code UpdateAssessmentUseCase} làm căn cứ — nhưng chỗ đó CHO PHÉP chủ tịch kỳ thi tập trung, nên căn
 * cứ ấy nói ngược lại điều kiện nó biện hộ. Vạch cũ còn tự mâu thuẫn theo mức độ phá huỷ: chủ tịch
 * kỳ thi tập trung vốn đã xoá được cả THÍ SINH ({@code DeleteAssessmentCandidateUseCase},
 * {@code BulkDeleteAssessmentCandidatesUseCase} đều không chặn theo loại kỳ thi), tức là gỡ được cả con
 * người, mà lại không gỡ nổi một lượt thi hỏng của chính người đó. Thao tác này an toàn hơn hẳn:
 * xoá mềm, bắt nhập lý do, và đóng lại khi kỳ thi đã chốt.
 */
@Service
public class DeleteAssessmentSessionUseCase implements IUseCase<DeleteAssessmentSessionCommand, Void> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;

    public DeleteAssessmentSessionUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentResultAppealRepository assessmentResultAppealRepository) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteAssessmentSessionCommand input) {
        var sessionId = input.sessionId();
        var session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra của phiên thi này"));

        authorizeDelete(assessment.getId(), assessment.getSchoolId());
        // Kỳ thi đã chốt sổ thì điểm đã (hoặc sắp) đến tay học sinh — xem Assessment#isResultsFinalized.
        if (assessment.isResultsFinalized()) {
            throw new IllegalStateException("Không thể xóa bài thi khi kỳ thi đã đóng hoặc đã công bố kết quả");
        }
        // Đơn phúc khảo đang mở là tranh chấp điểm mà học sinh đã chính thức nêu: xoá mềm kết quả
        // sẽ rút mất chính bài đang bị khiếu nại và để đơn treo trỏ vào một dòng đã ẩn.
        //
        // Chốt kỳ thi ở trên hiện đã chặn gần hết đường tới đây (đơn chỉ sinh ra sau khi điểm được
        // trả), nhưng nó canh vòng đời của KỲ THI còn đơn thì bám vào TỪNG bài — hai vòng đời khác
        // nhau, đừng để cái này ngầm suy ra cái kia. Nhánh chủ tịch vừa mở rộng làm số người bấm
        // được nút này đông hơn, nên chốt tường minh rẻ hơn là đi chứng minh nó bất khả thi.
        var candidateResult = assessmentCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        if (candidateResult != null
                && assessmentResultAppealRepository.existsOpenByCandidateResultId(candidateResult.getId())) {
            throw new IllegalStateException(
                "Bài thi này đang có đơn phúc khảo chưa xử lý xong. Hãy xử lý hoặc từ chối đơn trước khi xoá.");
        }

        var reason = input.reason() == null ? "" : input.reason().strip();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("Phải nhập lý do xóa bài thi");
        }

        var now = Instant.now();
        // Idempotent: hai người cùng bấm xoá thì lần sau nhận 0 dòng, và lý do/mốc thời gian của lần
        // đầu — thứ phải giữ nguyên để giải trình — không bị ghi đè.
        if (assessmentSessionRepository.softDelete(sessionId, now, reason) == 0) {
            return null;
        }
        // Điểm phải biến mất khỏi bảng kết quả, hàng đợi chấm và phúc khảo CÙNG LÚC với phiên thi.
        assessmentCandidateResultRepository.softDeleteBySessionId(sessionId, now, reason);
        return null;
    }

    private void authorizeDelete(UUID assessmentId, UUID assessmentSchoolId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
            return;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
