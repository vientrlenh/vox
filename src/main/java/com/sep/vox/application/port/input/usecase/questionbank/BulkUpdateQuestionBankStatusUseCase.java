package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.BulkUpdateQuestionScopeStatusCommand;
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
public class BulkUpdateQuestionBankStatusUseCase
        implements IUseCase<BulkUpdateQuestionScopeStatusCommand, BulkUpdateQuestionBankStatusResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public BulkUpdateQuestionBankStatusUseCase(
            QuestionBankRepository questionBankRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public BulkUpdateQuestionBankStatusResponse execute(BulkUpdateQuestionScopeStatusCommand input) {
        if (input.ids() == null || input.ids().isEmpty()) {
            throw new IllegalArgumentException("Danh sách ngân hàng câu hỏi không được để trống");
        }

        var action = StringNormalization.normalizeCode(input.action());
        // LinkedHashSet: giữ thứ tự client gửi lên để đọc response theo được, và bỏ id trùng để
        // không đổi hai lần cùng một mục.
        var ids = new LinkedHashSet<>(input.ids());

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var banksById = questionBankRepository.findByIdIn(ids).stream()
            .collect(Collectors.toMap(bank -> bank.getId(), bank -> bank, (first, second) -> first));

        var failed = new ArrayList<BulkUpdateQuestionScopeStatusFailure>();
        var toSave = new ArrayList<QuestionBank>();
        var now = Instant.now();

        for (var id : ids) {
            var bank = banksById.get(id);
            if (bank == null) {
                failed.add(new BulkUpdateQuestionScopeStatusFailure(
                    id, null, null,
                    RejectionCode.NOT_FOUND.name(),
                    QuestionScopeStatusTransition.NOT_FOUND_BANK));
                continue;
            }
            if (!canManage(bank, currentSchoolId)) {
                failed.add(failure(bank, RejectionCode.NO_PERMISSION.name(),
                    QuestionScopeStatusTransition.NO_PERMISSION));
                continue;
            }

            var rejection = QuestionScopeStatusTransition.rejectionFor(
                bank.getStatus().name(), action, "ngân hàng câu hỏi");
            if (rejection != null) {
                failed.add(failure(bank, rejection.code().name(), rejection.reason()));
                continue;
            }

            bank.setStatus(QuestionBankStatus.valueOf(QuestionScopeStatusTransition.nextStatusName(action)));
            bank.setUpdatedAt(now);
            bank.setUpdatedBy(currentUserId);
            toSave.add(bank);
        }

        // save() từng mục thay vì saveAll: repository này chưa có saveAll, mà trong cùng một
        // transaction thì số câu lệnh sinh ra không khác nhau.
        var updated = new ArrayList<com.sep.vox.domain.dto.QuestionBankDto>();
        for (var bank : toSave) {
            updated.add(QuestionBankDtoMapper.toDto(questionBankRepository.save(bank)));
        }

        return new BulkUpdateQuestionBankStatusResponse(List.copyOf(updated), List.copyOf(failed));
    }

    /** Cùng luật với UpdateQuestionBankStatusUseCase.validateAccess, chỉ đổi ném thành trả boolean. */
    private boolean canManage(QuestionBank bank, java.util.UUID currentSchoolId) {
        if (bank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            return userContextPort.isSystemAdmin();
        }
        return currentSchoolId != null && currentSchoolId.equals(bank.getSchoolId());
    }

    private BulkUpdateQuestionScopeStatusFailure failure(QuestionBank bank, String reasonCode, String reason) {
        return new BulkUpdateQuestionScopeStatusFailure(
            bank.getId(), bank.getCode(), bank.getStatus().name(), reasonCode, reason);
    }
}
