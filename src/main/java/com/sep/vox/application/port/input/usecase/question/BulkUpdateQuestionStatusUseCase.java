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
 * Updates the status of several questions in one request with "partial success" semantics:
 * every valid question is updated, every other one is returned in {@code failed} together with the reason.
 *
 * <p>This use case deliberately does NOT call {@link UpdateQuestionStatusUseCase}. It used to, and because
 * that use case is also {@code @Transactional} (propagation REQUIRED) it joined this very transaction;
 * the first rejected question made Spring mark the transaction rollback-only, the loop caught the
 * exception and returned normally, and at commit {@code UnexpectedRollbackException} blew up ->
 * HTTP 500, and the questions that had been updated successfully were lost as well. Both paths now share
 * {@link QuestionStatusTransition} -- a pure function that returns the rejection reason instead of throwing.
 *
 * <p>All the data needed is loaded in batches before the loop, so the number of queries does not depend on
 * the number of questions in the list.
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
            throw new IllegalArgumentException("The question list must not be empty");
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
     * Adds the question's code and current status to the skip reason, so the client can show a
     * "which question, where it is, why" list without looking it up in the list it is displaying.
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
