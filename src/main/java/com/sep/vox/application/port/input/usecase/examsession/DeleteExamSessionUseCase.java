package com.sep.vox.application.port.input.usecase.examsession;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRecordingRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Permanently deletes an exam session and every row it produced (item
 * responses, live turns, evaluations, criterion scores, evaluation turns,
 * candidate result rollup, and any appeal raised against that result) -- for
 * recovering from a session broken by a failed/errored exam entry.
 * Irreversible, no soft-delete/undo.
 *
 * Authorization: SCHOOL_ADMIN can delete any session for their own school
 * (any exam kind). A teacher can only delete a session for an exam they
 * chair AND only when that exam is CLASS_TEST -- centralized exams stay
 * school-admin-only, matching the mutation rules already enforced elsewhere
 * (UpdateExamUseCase.authorizeMutation).
 */
@Service
public class DeleteExamSessionUseCase implements IUseCase<UUID, Void> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository;
    private final ExamItemCriterionScoreRepository examItemCriterionScoreRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamResultAppealItemRepository examResultAppealItemRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamResultStatusHistoryRepository examResultStatusHistoryRepository;
    private final ExamRecordingRepository examRecordingRepository;

    public DeleteExamSessionUseCase(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamItemEvaluationTurnRepository examItemEvaluationTurnRepository,
            ExamItemCriterionScoreRepository examItemCriterionScoreRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamResultAppealRepository examResultAppealRepository,
            ExamResultAppealItemRepository examResultAppealItemRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamResultStatusHistoryRepository examResultStatusHistoryRepository,
            ExamRecordingRepository examRecordingRepository) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examItemEvaluationTurnRepository = examItemEvaluationTurnRepository;
        this.examItemCriterionScoreRepository = examItemCriterionScoreRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examResultAppealRepository = examResultAppealRepository;
        this.examResultAppealItemRepository = examResultAppealItemRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examResultStatusHistoryRepository = examResultStatusHistoryRepository;
        this.examRecordingRepository = examRecordingRepository;
    }

    @Override
    @Transactional
    public Void execute(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra của phiên thi này"));

        authorizeDelete(exam.getId(), exam.getSchoolId(), exam.getKind());

        var responseIds = examItemResponseRepository.findBySessionId(sessionId).stream()
            .map(response -> response.getId())
            .toList();

        if (!responseIds.isEmpty()) {
            var evaluationIds = examItemEvaluationRepository.findByResponseIdIn(responseIds).stream()
                .map(evaluation -> evaluation.getId())
                .toList();

            if (!evaluationIds.isEmpty()) {
                examItemCriterionScoreRepository.deleteByEvaluationIdIn(evaluationIds);
                examItemEvaluationTurnRepository.deleteByEvaluationIdIn(evaluationIds);
                examItemEvaluationRepository.deleteByResponseIdIn(responseIds);
            }

            examItemResponseTurnRepository.deleteByExamItemResponseIdIn(responseIds);
        }

        examItemResponseRepository.deleteBySessionId(sessionId);

        // Đơn phúc khảo treo trên candidate result, phải dọn TRƯỚC khi xoá nó.
        // Không có FK nào chặn, nên bỏ sót sẽ để lại đơn mồ côi trỏ vào kết quả
        // đã biến mất: đơn im lặng rơi khỏi mọi màn hình (INNER JOIN) nhưng dòng
        // vẫn nằm lại trong DB.
        examCandidateResultRepository.findBySessionId(sessionId).ifPresent(result -> {
            var appealIds = examResultAppealRepository.findByCandidateResultId(result.getId()).stream()
                .map(appeal -> appeal.getId())
                .toList();
            if (!appealIds.isEmpty()) {
                // Con trước cha: đơn có bảng con phần thi. Bỏ sót là để lại đúng loại
                // dòng mồ côi nói trên.
                examResultAppealItemRepository.deleteByAppealIdIn(appealIds);
                examResultAppealRepository.deleteByIdIn(appealIds);
            }
            // Phân công chấm và nhật ký trạng thái cũng treo trên candidate result và
            // cũng không có FK — hai bảng độc lập với nhau nên thứ tự giữa chúng không
            // quan trọng, nhưng cả hai phải xong trước khi candidate result biến mất.
            var resultIds = List.of(result.getId());
            examGradingAssignmentRepository.deleteByCandidateResultIdIn(resultIds);
            examResultStatusHistoryRepository.deleteByCandidateResultIdIn(resultIds);
        });

        examCandidateResultRepository.deleteBySessionId(sessionId);
        // Bản ghi hình/tiếng cũng treo trên phiên thi và cũng không có FK. Chỉ xoá dòng trong DB:
        // file trên S3 để lifecycle rule của bucket dọn, vì xoá file không rollback được cùng
        // transaction này.
        examRecordingRepository.deleteByExamSessionId(sessionId);
        examSessionRepository.deleteById(sessionId);
        return null;
    }

    private void authorizeDelete(UUID examId, UUID examSchoolId, ExamKind kind) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(examSchoolId)) {
            return;
        }
        if (kind == ExamKind.CLASS_TEST
                && examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
