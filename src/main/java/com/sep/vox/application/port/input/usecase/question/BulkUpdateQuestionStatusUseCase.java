package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.dto.QuestionDto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.question.BulkUpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusFailure;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusResponse;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.question.QuestionStatusTransition;
import com.sep.vox.domain.service.question.QuestionStatusTransition.RejectionCode;

/**
 * Cập nhật trạng thái nhiều câu hỏi trong một request, theo ngữ nghĩa "thành công một phần":
 * câu nào hợp lệ thì cập nhật, câu nào không thì trả về trong {@code failed} kèm lý do.
 *
 * <p>Use case này cố ý KHÔNG gọi lại {@link UpdateQuestionStatusUseCase}. Trước đây nó gọi, và vì
 * use case kia cũng {@code @Transactional} (propagation REQUIRED) nên nó tham gia chính transaction
 * này; câu hỏi đầu tiên bị từ chối làm Spring đánh dấu transaction rollback-only, vòng lặp bắt
 * exception rồi trả về bình thường, và lúc commit thì nổ {@code UnexpectedRollbackException} →
 * HTTP 500, đồng thời mất luôn những câu đã cập nhật thành công. Cả hai đường đi giờ dùng chung
 * {@link QuestionStatusTransition} — một hàm thuần trả về lý do từ chối thay vì ném exception.
 *
 * <p>Toàn bộ dữ liệu cần thiết được nạp theo lô trước vòng lặp, nên số query không phụ thuộc vào
 * số câu hỏi trong danh sách.
 */
@Service
@RequiredArgsConstructor
public class BulkUpdateQuestionStatusUseCase
        implements IUseCase<BulkUpdateQuestionStatusCommand, BulkUpdateQuestionStatusResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionStatusActorResolver questionStatusActorResolver;

    @Override
    @Transactional
    public BulkUpdateQuestionStatusResponse execute(BulkUpdateQuestionStatusCommand input) {
        if (input.questionIds() == null || input.questionIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách câu hỏi không được để trống");
        }

        String action = StringNormalization.normalizeCode(input.action());
        String note = StringNormalization.trimAndCollapseSpaces(input.note());
        // Keep the order the client sent so the response reads naturally, and drop duplicate ids
        // so the same question is not updated twice.
        Set<UUID> questionIds = new LinkedHashSet<>(input.questionIds());

        QuestionStatusTransition.Actor actor = questionStatusActorResolver.resolve();
        Map<UUID, Question> questionsById = questionRepository.findByIdIn(questionIds).stream()
            .collect(Collectors.toMap(question -> question.getId(), question -> question, (first, second) -> first));
        StatusUpdateContext context = new StatusUpdateContext(
            actor, action, note, loadBanks(questionsById), loadEditableQuestionIds(questionIds, actor.userId()));

        StatusUpdateOutcome outcome = applyTransitions(questionIds, questionsById, context);

        List<QuestionDto> updated = questionRepository.saveAll(outcome.toSave()).stream()
            .map(QuestionDtoMapper::toQuestionDto)
            .toList();

        return new BulkUpdateQuestionStatusResponse(List.copyOf(updated), List.copyOf(outcome.failed()));
    }

    private StatusUpdateOutcome applyTransitions(
            Set<UUID> questionIds, Map<UUID, Question> questionsById, StatusUpdateContext context) {
        List<BulkUpdateQuestionStatusFailure> failed = new ArrayList<BulkUpdateQuestionStatusFailure>();
        List<Question> toSave = new ArrayList<Question>();

        for (UUID questionId : questionIds) {
            Optional<BulkUpdateQuestionStatusFailure> failure = findFailure(questionId, questionsById, context);
            if (failure.isPresent()) {
                failed.add(failure.get());
                continue;
            }
            Question question = questionsById.get(questionId);
            QuestionStatusTransition.apply(question, context.action());
            toSave.add(question);
        }
        return new StatusUpdateOutcome(failed, toSave);
    }

    private Optional<BulkUpdateQuestionStatusFailure> findFailure(
            UUID questionId, Map<UUID, Question> questionsById, StatusUpdateContext context) {
        Question question = questionsById.get(questionId);
        if (question == null) {
            return Optional.of(new BulkUpdateQuestionStatusFailure(
                questionId, null, null,
                RejectionCode.QUESTION_NOT_FOUND.name(),
                QuestionStatusTransition.QUESTION_NOT_FOUND));
        }
        QuestionBank bank = context.banksById().get(question.getQuestionBankId());
        if (bank == null) {
            return Optional.of(failure(question, RejectionCode.QUESTION_BANK_NOT_FOUND.name(),
                QuestionStatusTransition.QUESTION_BANK_NOT_FOUND));
        }
        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(
            question, bank, context.editableQuestionIds().contains(questionId),
            context.actor(), context.action(), context.note());
        if (rejection != null) {
            return Optional.of(failure(question, rejection.code().name(), rejection.reason()));
        }
        return Optional.empty();
    }

    /**
     * Kèm mã và trạng thái hiện tại của câu hỏi vào lý do bị bỏ qua, để client hiển thị được danh
     * sách "câu nào, đang ở đâu, vì sao" mà không phải tra ngược sang danh sách đang xem.
     */
    private BulkUpdateQuestionStatusFailure failure(Question question, String reasonCode, String reason) {
        return new BulkUpdateQuestionStatusFailure(
            question.getId(), question.getCode(), question.getStatus().name(), reasonCode, reason);
    }

    private Map<UUID, QuestionBank> loadBanks(Map<UUID, Question> questionsById) {
        Set<UUID> bankIds = questionsById.values().stream()
            .map(question -> question.getQuestionBankId())
            .collect(Collectors.toSet());
        return questionBankRepository.findByIdIn(bankIds).stream()
            .collect(Collectors.toMap(bank -> bank.getId(), bank -> bank, (first, second) -> first));
    }

    private Set<UUID> loadEditableQuestionIds(Set<UUID> questionIds, UUID currentUserId) {
        return questionCollaboratorRepository.findByQuestionIdIn(questionIds).stream()
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .filter(collaborator -> collaborator.getUserId().equals(currentUserId))
            .map(collaborator -> collaborator.getQuestionId())
            .collect(Collectors.toSet());
    }

    private record StatusUpdateContext(
            QuestionStatusTransition.Actor actor,
            String action,
            String note,
            Map<UUID, QuestionBank> banksById,
            Set<UUID> editableQuestionIds) {
    }

    private record StatusUpdateOutcome(List<BulkUpdateQuestionStatusFailure> failed, List<Question> toSave) {
    }
}
