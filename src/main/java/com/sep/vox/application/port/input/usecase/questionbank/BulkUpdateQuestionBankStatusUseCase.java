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
 * Đổi trạng thái nhiều ngân hàng câu hỏi trong một request, theo ngữ nghĩa "thành công một phần":
 * mục nào hợp lệ thì đổi, mục nào không thì trả về trong {@code failed} kèm lý do.
 *
 * <p>Cố ý KHÔNG gọi lại {@link UpdateQuestionBankStatusUseCase}: use case kia ném exception khi từ
 * chối, mà nó cũng {@code @Transactional} nên sẽ tham gia chính transaction này -- mục đầu tiên bị
 * từ chối là Spring đánh dấu rollback-only, và lúc commit nổ {@code UnexpectedRollbackException},
 * cuốn theo cả những mục đã đổi thành công. Cả hai đường giờ dùng chung
 * {@link QuestionScopeStatusTransition}, một hàm thuần trả về lý do thay vì ném.
 *
 * <p>Nạp theo lô trước vòng lặp nên số query không phụ thuộc số mục được chọn.
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
            throw new IllegalArgumentException("Danh sách ngân hàng câu hỏi không được để trống");
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
            bank.getStatus().name(), action, "ngân hàng câu hỏi");
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

    /** Cùng luật với UpdateQuestionBankStatusUseCase.validateAccess, chỉ đổi ném thành trả boolean. */
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
