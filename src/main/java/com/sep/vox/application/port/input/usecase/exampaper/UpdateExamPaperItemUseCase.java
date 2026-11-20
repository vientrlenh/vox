package com.sep.vox.application.port.input.usecase.exampaper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamPaperItemCommand;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.mapper.ExamPaperItemDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.exam.PaperTimeCalculator;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
public class UpdateExamPaperItemUseCase implements IUseCase<UpdateExamPaperItemCommand, ExamPaperItemDto> {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamPaperAuthoringAccessService examPaperAuthoringAccessService;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final ExamTimeQuotaGuardService examTimeQuotaGuardService;
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public UpdateExamPaperItemUseCase(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamPaperAuthoringAccessService examPaperAuthoringAccessService,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            SchoolUserRepository schoolUserRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            ExamTimeQuotaGuardService examTimeQuotaGuardService,
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examPaperAuthoringAccessService = examPaperAuthoringAccessService;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.examTimeQuotaGuardService = examTimeQuotaGuardService;
        this.recalculateExamTimeDurationService = recalculateExamTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperItemDto execute(UpdateExamPaperItemCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var item = examPaperItemRepository.findById(input.itemId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong đề thi"));
        if (!item.getPaperId().equals(input.paperId())) {
            throw new NotFoundException("Không tìm thấy câu hỏi trong đề thi");
        }
        var paper = examPaperRepository.findById(item.getPaperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var exam = examRepository.findById(paper.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        // Bài trên lớp khoá đề bằng một bước DRAFT → LOCKED và mở lại tự do, nên LOCKED ở đó là trạng
        // thái làm việc bình thường; chỉ ở kỳ thi tập trung nó mới mang nghĩa "chốt, không sửa nữa".
        if (exam.getKind() == ExamKind.CENTRALIZED && paper.getStatus() == ExamPaperStatus.LOCKED) {
            throw new IllegalStateException("Đề thi đã bị khoá, không thể sửa câu hỏi");
        }
        if (exam.getStatus() == ExamStatus.IN_PROGRESS) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi bài kiểm tra đang diễn ra");
        }
        examPaperAuthoringAccessService.requireCanAuthor(exam, currentUserId);

        // H.7: chỉ ô FIXED mới phải khớp y hệt blueprint - đổi câu ở đó bắt buộc qua đổi version.
        // Ô SELECTION thì blueprint chỉ mô tả tiêu chí, người ra đề mới là người chọn câu cụ thể,
        // nên đây chính là chỗ duy nhất để gán câu cho ô đó.
        //
        // Slot đã bị xoá (version còn DRAFT vẫn xoá slot được) thì coi như ô tự do: ràng buộc
        // blueprint không còn tồn tại nữa, fail-closed ở đây chỉ làm mã đề kẹt vĩnh viễn.
        var slot = item.getBlueprintSlotId() == null
            ? null
            : examBlueprintSlotRepository.findById(item.getBlueprintSlotId()).orElse(null);
        if (slot != null && slot.getSlotType() == ExamBlueprintSlotType.FIXED) {
            throw new IllegalStateException(
                "Ô câu hỏi này cố định theo blueprint, không thể đổi câu hỏi trực tiếp - phải qua đổi version blueprint");
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var question = questionRepository.findAccessibleById(input.questionId(), currentUserId, currentSchoolId, false, false)
            .orElseThrow(() -> new ForbiddenException("Không có quyền dùng câu hỏi này"));
        if ((exam.getKind() == ExamKind.CENTRALIZED || slot != null)
                && question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được gán câu hỏi đã PUBLISHED vào đề thi");
        }
        if (slot != null && slot.getSlotType() == ExamBlueprintSlotType.SELECTION) {
            requireMatchesSelectionSpec(slot.getSelectionSpec(), question);
        }

        boolean isOwner = currentUserId.equals(question.getCreatedBy());
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (!isOwner && !isSchoolShared) {
            var collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
            if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                throw new ForbiddenException("Quyền READ_ONLY không được phép gán câu hỏi vào đề thi");
            }
        }

        var candidateDurationSeconds = calculatePaperDurationAfterQuestionChange(paper.getId(), item.getId(), question);
        examTimeQuotaGuardService.requireWithinPlan(
            exam.getSchoolId(),
            candidateDurationSeconds,
            "Mã đề " + paper.getCode()
        );

        item.setQuestionId(question.getId());
        var savedItem = examPaperItemRepository.save(item);

        examQuestionSecureLockService.lockQuestionForExam(
            question.getId(),
            paper.getExamId(),
            releaseModeFor(exam),
            currentUserId
        );

        if (paper.getStatus() == ExamPaperStatus.APPROVED) {
            paper.setStatus(ExamPaperStatus.IN_REVIEW);
        }
        paper.setUpdatedAt(Instant.now());
        paper.setUpdatedBy(currentUserId);
        examPaperRepository.save(paper);

        recalculateExamTimeDurationService.recalculate(paper.getExamId());
        return ExamPaperItemDtoMapper.toDto(savedItem);
    }

    /**
     * Câu hỏi của bài trên lớp tự mở khoá khi đóng bài; kỳ thi tập trung do người quản lý tự mở.
     */
    private ExamSecurePoolReleaseMode releaseModeFor(Exam exam) {
        return exam.getKind() == ExamKind.CLASS_TEST
            ? ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE
            : ExamSecurePoolReleaseMode.MANUAL;
    }

    /**
     * Ô SELECTION chỉ mô tả tiêu chí, nên câu được gán phải khớp tiêu chí đó. Picker ở client đã
     * lọc sẵn, nhưng endpoint REST gọi thẳng được nên vẫn phải chặn ở đây.
     *
     * <p>Chỉ kiểm được {@code questionType} và {@code topicId}: {@link com.sep.vox.domain.model.question.Question}
     * không có difficulty/skillCode/targetBandLevel nên 3 tiêu chí còn lại chỉ mang tính tham khảo
     * cho người ra đề.
     */
    private void requireMatchesSelectionSpec(QuestionSelectionSpec spec, Question question) {
        if (spec == null) {
            return;
        }
        if (spec.questionType() != null && question.getType() != spec.questionType()) {
            throw new IllegalStateException(
                "Ô câu hỏi này yêu cầu câu hỏi loại " + spec.questionType() + ", câu hỏi đã chọn không khớp tiêu chí");
        }
        if (spec.topicId() != null && !spec.topicId().equals(question.getQuestionTopicId())) {
            throw new IllegalStateException("Câu hỏi đã chọn không thuộc chủ đề mà ô câu hỏi yêu cầu");
        }
    }

    private int calculatePaperDurationAfterQuestionChange(
            UUID paperId,
            UUID itemId,
            Question replacementQuestion) {
        // Chiếu thời lượng của mã đề SAU khi thay câu, TRƯỚC khi ghi -- nên không dùng lại được
        // RecalculateExamTimeDurationService (nó đọc từ DB, mà thay đổi chưa nằm trong đó).
        var questions = new ArrayList<Question>();
        for (var candidateItem : examPaperItemRepository.findByPaperId(paperId)) {
            if (candidateItem.getId().equals(itemId)) {
                questions.add(replacementQuestion);
                continue;
            }
            if (candidateItem.getQuestionId() == null) {
                continue;
            }
            questionRepository.findById(candidateItem.getQuestionId()).ifPresent(questions::add);
        }
        // totalSeconds (đã gồm thời lượng phát AUDIO/VIDEO) vì đây là thước đo ĐỘ DÀI so với gói --
        // xem PaperTimeCalculator để rõ vì sao ước tính CHI PHÍ lại dùng số khác.
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(q -> q.getId()).distinct().toList()));
        return PaperTimeCalculator.breakdownOf(questions, assetByQuestionId).totalSeconds();
    }
}
