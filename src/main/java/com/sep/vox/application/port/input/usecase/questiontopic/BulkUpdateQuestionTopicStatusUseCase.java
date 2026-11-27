package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.dto.QuestionTopicDto;
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
import com.sep.vox.application.port.input.command.questionbank.BulkUpdateQuestionScopeStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionScopeStatusFailure;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionTopicStatusResponse;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.question.QuestionScopeStatusTransition;
import com.sep.vox.domain.service.question.QuestionScopeStatusTransition.RejectionCode;

/**
 * Changes the status of several question topics in one request, "partial success" -- see the javadoc of
 * {@code BulkUpdateQuestionBankStatusUseCase} for why the single-item use case is not called again.
 *
 * <p>This is the step people trip over most when loading data: question import ONLY accepts {@code PUBLISHED} topics,
 * while topics imported from Excel always arrive as {@code DRAFT}. Without bulk publish every topic has to be opened
 * one by one -- three grades means thirty clicks.
 */
@Service
@RequiredArgsConstructor
public class BulkUpdateQuestionTopicStatusUseCase
        implements IUseCase<BulkUpdateQuestionScopeStatusCommand, BulkUpdateQuestionTopicStatusResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public BulkUpdateQuestionTopicStatusResponse execute(BulkUpdateQuestionScopeStatusCommand input) {
        if (input.ids() == null || input.ids().isEmpty()) {
            throw new IllegalArgumentException("The question topic list must not be empty");
        }

        String action = StringNormalization.normalizeCode(input.action());
        Set<UUID> ids = new LinkedHashSet<>(input.ids());

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        Map<UUID, QuestionTopic> topicsById = questionTopicRepository.findByIdIn(ids).stream()
            .collect(Collectors.toMap(topic -> topic.getId(), topic -> topic, (first, second) -> first));
        // The permission on a topic derives from the bank that holds it, so the banks are loaded too --
        // still one query for the whole batch.
        StatusUpdateContext context = new StatusUpdateContext(action, currentSchoolId, loadBanks(topicsById));

        StatusUpdateOutcome outcome = applyTransitions(ids, topicsById, context);
        List<QuestionTopicDto> updated = saveEach(outcome.toSave());

        return new BulkUpdateQuestionTopicStatusResponse(List.copyOf(updated), List.copyOf(outcome.failed()));
    }

    private StatusUpdateOutcome applyTransitions(
            Set<UUID> ids, Map<UUID, QuestionTopic> topicsById, StatusUpdateContext context) {
        List<BulkUpdateQuestionScopeStatusFailure> failed = new ArrayList<BulkUpdateQuestionScopeStatusFailure>();
        List<QuestionTopic> toSave = new ArrayList<QuestionTopic>();

        for (UUID id : ids) {
            Optional<BulkUpdateQuestionScopeStatusFailure> failure = findFailure(id, topicsById, context);
            if (failure.isPresent()) {
                failed.add(failure.get());
                continue;
            }
            QuestionTopic topic = topicsById.get(id);
            topic.setStatus(QuestionTopicStatus.valueOf(QuestionScopeStatusTransition.nextStatusName(context.action())));
            toSave.add(topic);
        }
        return new StatusUpdateOutcome(failed, toSave);
    }

    private Optional<BulkUpdateQuestionScopeStatusFailure> findFailure(
            UUID id, Map<UUID, QuestionTopic> topicsById, StatusUpdateContext context) {
        QuestionTopic topic = topicsById.get(id);
        if (topic == null) {
            return Optional.of(new BulkUpdateQuestionScopeStatusFailure(
                id, null, null,
                RejectionCode.NOT_FOUND.name(),
                QuestionScopeStatusTransition.NOT_FOUND_TOPIC));
        }
        QuestionBank bank = context.banksById().get(topic.getQuestionBankId());
        if (bank == null || !canManage(bank, context.currentSchoolId())) {
            return Optional.of(failure(topic, RejectionCode.NO_PERMISSION.name(),
                QuestionScopeStatusTransition.NO_PERMISSION));
        }

        QuestionScopeStatusTransition.Rejection rejection = QuestionScopeStatusTransition.rejectionFor(
            topic.getStatus().name(), context.action(), "question topic");
        if (rejection != null) {
            return Optional.of(failure(topic, rejection.code().name(), rejection.reason()));
        }
        return Optional.empty();
    }

    /**
     * See BulkUpdateQuestionBankStatusUseCase: the repository has no saveAll, and inside one transaction
     * saving one by one generates no extra statement.
     */
    private List<QuestionTopicDto> saveEach(List<QuestionTopic> toSave) {
        List<QuestionTopicDto> updated = new ArrayList<QuestionTopicDto>();
        for (QuestionTopic topic : toSave) {
            updated.add(QuestionTopicDtoMapper.toDto(questionTopicRepository.save(topic)));
        }
        return updated;
    }

    private Map<UUID, QuestionBank> loadBanks(Map<UUID, QuestionTopic> topicsById) {
        Set<UUID> bankIds = topicsById.values().stream()
            .map(topic -> topic.getQuestionBankId())
            .collect(Collectors.toSet());
        if (bankIds.isEmpty()) {
            return Map.of();
        }
        return questionBankRepository.findByIdIn(bankIds).stream()
            .collect(Collectors.toMap(bank -> bank.getId(), bank -> bank, (first, second) -> first));
    }

    private boolean canManage(QuestionBank bank, UUID currentSchoolId) {
        if (bank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            return userContextPort.isSystemAdmin();
        }
        return currentSchoolId != null && currentSchoolId.equals(bank.getSchoolId());
    }

    private BulkUpdateQuestionScopeStatusFailure failure(QuestionTopic topic, String reasonCode, String reason) {
        return new BulkUpdateQuestionScopeStatusFailure(
            topic.getId(), topic.getCode(), topic.getStatus().name(), reasonCode, reason);
    }

    private record StatusUpdateContext(String action, UUID currentSchoolId, Map<UUID, QuestionBank> banksById) {
    }

    private record StatusUpdateOutcome(
            List<BulkUpdateQuestionScopeStatusFailure> failed, List<QuestionTopic> toSave) {
    }
}
