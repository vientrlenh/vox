package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.dto.QuestionBankDto;
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
import com.sep.vox.application.response.input.question.BulkUpdateQuestionBankStatusResponse;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionScopeStatusFailure;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.question.QuestionScopeStatusTransition;
import com.sep.vox.domain.service.question.QuestionScopeStatusTransition.RejectionCode;

/**
 * Changes the status of several question banks in one request with "partial success" semantics:
 * every valid item is changed, every other one is returned in {@code failed} together with the reason.
 *
 * <p>Deliberately does NOT call {@link UpdateQuestionBankStatusUseCase}: that use case throws when it
 * rejects, and because it is also {@code @Transactional} it joins this very transaction -- the first
 * rejected item makes Spring mark it rollback-only, and at commit {@code UnexpectedRollbackException} blows up,
 * taking the items that were changed successfully with it. Both paths now share
 * {@link QuestionScopeStatusTransition}, a pure function that returns the reason instead of throwing.
 *
 * <p>Loaded in batches before the loop, so the number of queries does not depend on the number of selected items.
 */
@Service
@RequiredArgsConstructor
public class BulkUpdateQuestionBankStatusUseCase
        implements IUseCase<BulkUpdateQuestionScopeStatusCommand, BulkUpdateQuestionBankStatusResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public BulkUpdateQuestionBankStatusResponse execute(BulkUpdateQuestionScopeStatusCommand input) {
        if (input.ids() == null || input.ids().isEmpty()) {
            throw new IllegalArgumentException("The question bank list must not be empty");
        }

        String action = StringNormalization.normalizeCode(input.action());
        // LinkedHashSet keeps the order the client sent so the response reads naturally, and drops duplicate
        // ids so the same item is not changed twice.
        Set<UUID> ids = new LinkedHashSet<>(input.ids());

        UUID currentSchoolId = resolveCurrentSchoolId();

        Map<UUID, QuestionBank> banksById = questionBankRepository.findByIdIn(ids).stream()
            .collect(Collectors.toMap(bank -> bank.getId(), bank -> bank, (first, second) -> first));

        StatusUpdateOutcome outcome = applyTransitions(ids, banksById, action, currentSchoolId);
        List<QuestionBankDto> updated = saveEach(outcome.toSave());

        return new BulkUpdateQuestionBankStatusResponse(List.copyOf(updated), List.copyOf(outcome.failed()));
    }

    private UUID resolveCurrentSchoolId() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        return schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
    }

    private StatusUpdateOutcome applyTransitions(
            Set<UUID> ids, Map<UUID, QuestionBank> banksById, String action, UUID currentSchoolId) {
        List<BulkUpdateQuestionScopeStatusFailure> failed = new ArrayList<BulkUpdateQuestionScopeStatusFailure>();
        List<QuestionBank> toSave = new ArrayList<QuestionBank>();

        for (UUID id : ids) {
            Optional<BulkUpdateQuestionScopeStatusFailure> failure = findFailure(id, banksById, action, currentSchoolId);
            if (failure.isPresent()) {
                failed.add(failure.get());
                continue;
            }
            QuestionBank bank = banksById.get(id);
            bank.setStatus(QuestionBankStatus.valueOf(QuestionScopeStatusTransition.nextStatusName(action)));
            toSave.add(bank);
        }
        return new StatusUpdateOutcome(failed, toSave);
    }

    private Optional<BulkUpdateQuestionScopeStatusFailure> findFailure(
            UUID id, Map<UUID, QuestionBank> banksById, String action, UUID currentSchoolId) {
        QuestionBank bank = banksById.get(id);
        if (bank == null) {
            return Optional.of(new BulkUpdateQuestionScopeStatusFailure(
                id, null, null,
                RejectionCode.NOT_FOUND.name(),
                QuestionScopeStatusTransition.NOT_FOUND_BANK));
        }
        if (!canManage(bank, currentSchoolId)) {
            return Optional.of(failure(bank, RejectionCode.NO_PERMISSION.name(),
                QuestionScopeStatusTransition.NO_PERMISSION));
        }

        QuestionScopeStatusTransition.Rejection rejection = QuestionScopeStatusTransition.rejectionFor(
            bank.getStatus().name(), action, "question bank");
        if (rejection != null) {
            return Optional.of(failure(bank, rejection.code().name(), rejection.reason()));
        }
        return Optional.empty();
    }

    /**
     * Saves one by one instead of saveAll: this repository has no saveAll, and inside one transaction the
     * number of generated statements is the same.
     */
    private List<QuestionBankDto> saveEach(List<QuestionBank> toSave) {
        List<QuestionBankDto> updated = new ArrayList<QuestionBankDto>();
        for (QuestionBank bank : toSave) {
            updated.add(QuestionBankDtoMapper.toDto(questionBankRepository.save(bank)));
        }
        return updated;
    }

    /** Same rule as UpdateQuestionBankStatusUseCase.validateAccess, except it returns a boolean instead of throwing. */
    private boolean canManage(QuestionBank bank, UUID currentSchoolId) {
        if (bank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            return userContextPort.isSystemAdmin();
        }
        return currentSchoolId != null && currentSchoolId.equals(bank.getSchoolId());
    }

    private BulkUpdateQuestionScopeStatusFailure failure(QuestionBank bank, String reasonCode, String reason) {
        return new BulkUpdateQuestionScopeStatusFailure(
            bank.getId(), bank.getCode(), bank.getStatus().name(), reasonCode, reason);
    }

    private record StatusUpdateOutcome(
            List<BulkUpdateQuestionScopeStatusFailure> failed, List<QuestionBank> toSave) {
    }
}
