package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.BulkUpdateQuestionScopeStatusCommand;
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
 * Đổi trạng thái nhiều chủ đề câu hỏi trong một request, "thành công một phần" -- xem javadoc của
 * {@code BulkUpdateQuestionBankStatusUseCase} để rõ vì sao không gọi lại use case đổi từng mục.
 *
 * <p>Đây là bước hay vấp nhất khi nạp dữ liệu: import câu hỏi CHỈ nhận chủ đề đã {@code PUBLISHED},
 * mà chủ đề nhập từ Excel luôn vào ở {@code DRAFT}. Không có publish hàng loạt thì phải mở từng chủ
 * đề một -- ba khối là ba chục lần bấm.
 */
@Service
public class BulkUpdateQuestionTopicStatusUseCase
        implements IUseCase<BulkUpdateQuestionScopeStatusCommand, BulkUpdateQuestionTopicStatusResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public BulkUpdateQuestionTopicStatusUseCase(
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public BulkUpdateQuestionTopicStatusResponse execute(BulkUpdateQuestionScopeStatusCommand input) {
        if (input.ids() == null || input.ids().isEmpty()) {
            throw new IllegalArgumentException("Danh sách chủ đề câu hỏi không được để trống");
        }

        var action = StringNormalization.normalizeCode(input.action());
        var ids = new LinkedHashSet<>(input.ids());

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var topicsById = questionTopicRepository.findByIdIn(ids).stream()
            .collect(Collectors.toMap(topic -> topic.getId(), topic -> topic, (first, second) -> first));
        // Quyền của chủ đề suy từ ngân hàng chứa nó, nên phải nạp kèm -- vẫn một query cho cả lô.
        var banksById = loadBanks(topicsById);

        var failed = new ArrayList<BulkUpdateQuestionScopeStatusFailure>();
        var toSave = new ArrayList<QuestionTopic>();
        var now = Instant.now();

        for (var id : ids) {
            var topic = topicsById.get(id);
            if (topic == null) {
                failed.add(new BulkUpdateQuestionScopeStatusFailure(
                    id, null, null,
                    RejectionCode.NOT_FOUND.name(),
                    QuestionScopeStatusTransition.NOT_FOUND_TOPIC));
                continue;
            }
            var bank = banksById.get(topic.getQuestionBankId());
            if (bank == null || !canManage(bank, currentSchoolId)) {
                failed.add(failure(topic, RejectionCode.NO_PERMISSION.name(),
                    QuestionScopeStatusTransition.NO_PERMISSION));
                continue;
            }

            var rejection = QuestionScopeStatusTransition.rejectionFor(
                topic.getStatus().name(), action, "chủ đề câu hỏi");
            if (rejection != null) {
                failed.add(failure(topic, rejection.code().name(), rejection.reason()));
                continue;
            }

            topic.setStatus(QuestionTopicStatus.valueOf(QuestionScopeStatusTransition.nextStatusName(action)));
            topic.setUpdatedAt(now);
            topic.setUpdatedBy(currentUserId);
            toSave.add(topic);
        }

        // Xem BulkUpdateQuestionBankStatusUseCase: repository chưa có saveAll, và trong cùng một
        // transaction thì lưu từng mục không sinh thêm câu lệnh nào.
        var updated = new ArrayList<com.sep.vox.domain.dto.QuestionTopicDto>();
        for (var topic : toSave) {
            updated.add(QuestionTopicDtoMapper.toDto(questionTopicRepository.save(topic)));
        }

        return new BulkUpdateQuestionTopicStatusResponse(List.copyOf(updated), List.copyOf(failed));
    }

    private Map<UUID, QuestionBank> loadBanks(Map<UUID, QuestionTopic> topicsById) {
        var bankIds = topicsById.values().stream()
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
}
