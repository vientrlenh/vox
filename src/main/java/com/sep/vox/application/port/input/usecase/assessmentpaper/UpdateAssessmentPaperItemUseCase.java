package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentPaperItemCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperItemDto;
import com.sep.vox.domain.mapper.AssessmentPaperItemDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentPaperItemUseCase implements IUseCase<UpdateAssessmentPaperItemCommand, AssessmentPaperItemDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentPaperItemDto execute(UpdateAssessmentPaperItemCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentPaperItem item = assessmentPaperItemRepository.findById(input.itemId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong đề thi"));
        if (!item.getPaperId().equals(input.paperId())) {
            throw new NotFoundException("Không tìm thấy câu hỏi trong đề thi");
        }
        AssessmentPaper paper = assessmentPaperRepository.findById(item.getPaperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        Assessment assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        // Bài trên lớp khoá đề bằng một bước DRAFT → LOCKED và mở lại tự do, nên LOCKED ở đó là trạng
        // thái làm việc bình thường; chỉ ở kỳ thi tập trung nó mới mang nghĩa "chốt, không sửa nữa".
        if (assessment.getKind() == AssessmentKind.CENTRALIZED && paper.getStatus() == AssessmentPaperStatus.LOCKED) {
            throw new IllegalStateException("Đề thi đã bị khoá, không thể sửa câu hỏi");
        }
        if (assessment.getStatus() == AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi bài kiểm tra đang diễn ra");
        }
        assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);

        // H.7: chỉ ô FIXED mới phải khớp y hệt blueprint - đổi câu ở đó bắt buộc qua đổi version.
        // Ô SELECTION thì blueprint chỉ mô tả tiêu chí, người ra đề mới là người chọn câu cụ thể,
        // nên đây chính là chỗ duy nhất để gán câu cho ô đó.
        //
        // Slot đã bị xoá (version còn DRAFT vẫn xoá slot được) thì coi như ô tự do: ràng buộc
        // blueprint không còn tồn tại nữa, fail-closed ở đây chỉ làm mã đề kẹt vĩnh viễn.
        AssessmentBlueprintSlot slot = item.getBlueprintSlotId() == null
            ? null
            : assessmentBlueprintSlotRepository.findById(item.getBlueprintSlotId()).orElse(null);
        if (slot != null && slot.getSlotType() == AssessmentBlueprintSlotType.FIXED) {
            throw new IllegalStateException(
                "Ô câu hỏi này cố định theo blueprint, không thể đổi câu hỏi trực tiếp - phải qua đổi version blueprint");
        }

        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        Question question = questionRepository.findAccessibleById(input.questionId(), currentUserId, currentSchoolId, false, false)
            .orElseThrow(() -> new ForbiddenException("Không có quyền dùng câu hỏi này"));
        if ((assessment.getKind() == AssessmentKind.CENTRALIZED || slot != null)
                && question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được gán câu hỏi đã PUBLISHED vào đề thi");
        }
        if (slot != null && slot.getSlotType() == AssessmentBlueprintSlotType.SELECTION) {
            requireMatchesSelectionSpec(slot.getSelectionSpec(), question);
        }

        boolean isOwner = question.isCreatedBy(currentUserId);
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (!isOwner && !isSchoolShared) {
            Optional<QuestionCollaborator> collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
            if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                throw new ForbiddenException("Quyền READ_ONLY không được phép gán câu hỏi vào đề thi");
            }
        }

        int candidateDurationSeconds = calculatePaperDurationAfterQuestionChange(paper.getId(), item.getId(), question);
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            candidateDurationSeconds,
            "Mã đề " + paper.getCode()
        );

        item.setQuestionId(question.getId());
        AssessmentPaperItem savedItem = assessmentPaperItemRepository.save(item);

        assessmentQuestionSecureLockService.lockQuestionForAssessment(
            question.getId(),
            paper.getAssessmentId(),
            releaseModeFor(assessment),
            currentUserId
        );

        if (paper.getStatus() == AssessmentPaperStatus.APPROVED) {
            paper.setStatus(AssessmentPaperStatus.IN_REVIEW);
        }
        assessmentPaperRepository.save(paper);

        recalculateAssessmentTimeDurationService.recalculate(paper.getAssessmentId());
        return AssessmentPaperItemDtoMapper.toDto(savedItem);
    }

    /**
     * Câu hỏi của bài trên lớp tự mở khoá khi đóng bài; kỳ thi tập trung do người quản lý tự mở.
     */
    private AssessmentSecurePoolReleaseMode releaseModeFor(Assessment assessment) {
        return assessment.getKind() == AssessmentKind.CLASS_TEST
            ? AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE
            : AssessmentSecurePoolReleaseMode.MANUAL;
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
        // Projects the duration of the paper variant AFTER the question swap, BEFORE writing -- so
        // RecalculateAssessmentTimeDurationService cannot be reused (it reads from the DB, and the change is not there yet).
        List<Question> questions = new ArrayList<Question>();
        for (AssessmentPaperItem candidateItem : assessmentPaperItemRepository.findByPaperId(paperId)) {
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
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(q -> q.getId()).distinct().toList()));
        return PaperTimeCalculator.breakdownOf(questions, assetByQuestionId).totalSeconds();
    }
}
