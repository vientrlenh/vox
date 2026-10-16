package com.sep.vox.application.port.input.usecase.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.ChangeClassTestBlueprintCommand;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class ChangeClassTestBlueprintUseCase implements IUseCase<ChangeClassTestBlueprintCommand, AssessmentDto> {

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    public ChangeClassTestBlueprintUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService,
            RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionRepository = questionRepository;
        this.assessmentQuestionSecureLockService = assessmentQuestionSecureLockService;
        this.assessmentTimeQuotaGuardService = assessmentTimeQuotaGuardService;
        this.recalculateAssessmentTimeDurationService = recalculateAssessmentTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentDto execute(ChangeClassTestBlueprintCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra trên lớp");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ được đổi blueprint khi bài kiểm tra chưa bắt đầu");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessment.getId())) {
            throw new IllegalStateException("Không thể đổi blueprint khi đã có học sinh nộp bài");
        }
        if ((input.blueprintId() == null) != (input.blueprintVersionId() == null)) {
            throw new IllegalStateException(
                "Phải cung cấp đầy đủ blueprintId và blueprintVersionId, hoặc để trống cả hai để gỡ blueprint");
        }
        if (input.blueprintId() != null) {
            validateExistingBlueprintDuration(input, assessment);
        }

        var now = Instant.now();
        // Đổi blueprint viết lại toàn bộ nội dung của mã đề. Bài có nhiều mã đề thì không có mã đề
        // nào là "mã đề của blueprint" cả — chặn hẳn thay vì âm thầm ghi đè mã đề đầu tiên.
        var papers = assessmentPaperRepository.findByAssessmentId(assessment.getId());
        if (papers.size() > 1) {
            throw new IllegalStateException(
                "Bài có nhiều mã đề, hãy xoá bớt chỉ còn một mã đề trước khi đổi blueprint");
        }
        var paper = papers.stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Bài kiểm tra chưa có mã đề nào"));

        clearExistingPaperContent(paper, currentUserId);

        if (input.blueprintId() != null) {
            switchToExistingBlueprint(input, assessment, paper, currentUserId, now);
        } else {
            // Gỡ blueprint: để trống hoàn toàn, không tạo blueprint ẩn nào — bài trở về chế độ câu hỏi trực tiếp tự do.
            assessment.setBlueprintId(null);
            assessment.setBlueprintVersionId(null);
        }

        assessment.setUpdatedAt(now);
        assessment.setUpdatedBy(currentUserId);
        var saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDto.toDto(saved);
    }

    private void validateExistingBlueprintDuration(ChangeClassTestBlueprintCommand input, Assessment assessment) {
        var blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint"));
        if (!blueprint.getSchoolId().equals(assessment.getSchoolId())) {
            throw new IllegalStateException("Blueprint không thuộc trường của giáo viên");
        }
        var version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        if (!version.getBlueprintId().equals(blueprint.getId())) {
            throw new IllegalStateException("Version không thuộc blueprint đã chọn");
        }
        if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được dùng version đã PUBLISHED");
        }
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            version.getTotalTimeLimitSeconds(),
            "Phiên bản blueprint " + version.getCode()
        );
    }

    private void clearExistingPaperContent(AssessmentPaper paper, UUID currentUserId) {
        for (var paperSection : assessmentPaperSectionRepository.findByPaperId(paper.getId())) {
            for (var item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
                if (item.getQuestionId() != null) {
                    assessmentQuestionSecureLockService.unlockQuestion(item.getQuestionId(), currentUserId);
                }
                assessmentPaperItemRepository.deleteById(item.getId());
            }
            assessmentPaperSectionRepository.deleteById(paperSection.getId());
        }
    }

    private void switchToExistingBlueprint(
            ChangeClassTestBlueprintCommand input,
            Assessment assessment,
            AssessmentPaper paper,
            UUID currentUserId,
            Instant now) {
        var blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint"));
        if (!blueprint.getSchoolId().equals(assessment.getSchoolId())) {
            throw new IllegalStateException("Blueprint không thuộc trường của giáo viên");
        }
        var version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        if (!version.getBlueprintId().equals(blueprint.getId())) {
            throw new IllegalStateException("Version không thuộc blueprint đã chọn");
        }
        if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được dùng version đã PUBLISHED");
        }

        var sections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        if (sections.isEmpty()) {
            throw new IllegalStateException("Blueprint version không có section nào");
        }
        var slotsBySectionId = assessmentBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));
        validateVersionWeights(sections, slotsBySectionId);
        validateReusableSlots(slotsBySectionId.values().stream().flatMap(list -> list.stream()).toList());

        assessment.setBlueprintId(blueprint.getId());
        assessment.setBlueprintVersionId(version.getId());

        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();
            var paperSection = assessmentPaperSectionRepository.save(new AssessmentPaperSection(
                paper.getId(),
                section.getOrder(),
                section.getTitle(),
                section.getInstruction(),
                section.getSectionTimeLimitSeconds(),
                section.getSectionWeight(),
                now,
                now,
                currentUserId,
                currentUserId
            ));
            for (var slot : slots) {
                var questionId = slot.getSlotType() == AssessmentBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                if (questionId != null) {
                    var fixedQuestion = questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi cố định trong blueprint"));
                    if (fixedQuestion.getStatus() != QuestionStatus.PUBLISHED) {
                        // Câu hỏi fixed đã bị archived sau khi blueprint publish — để trống ô này, CHAIR tự chọn câu khác qua picker.
                        assessmentPaperItemRepository.save(new com.sep.vox.domain.model.assessment.AssessmentPaperItem(
                            null,
                            paperSection.getId(),
                            paper.getId(),
                            null,
                            slot.getOrder(),
                            slot.getWeight()
                        ));
                        continue;
                    }
                }
                var item = new com.sep.vox.domain.model.assessment.AssessmentPaperItem(
                    slot.getId(),
                    paperSection.getId(),
                    paper.getId(),
                    questionId,
                    slot.getOrder(),
                    slot.getWeight()
                );
                assessmentPaperItemRepository.save(item);
                if (questionId != null) {
                    assessmentQuestionSecureLockService.lockQuestionForAssessment(
                        questionId, assessment.getId(), AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
                    );
                }
            }
        }
    }

    private void validateVersionWeights(List<AssessmentBlueprintSection> sections, Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId) {
        var sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException(
                "Blueprint version đã chốt có tổng trọng số section không hợp lệ, không thể dùng cho bài kiểm tra");
        }
        for (var section : sections) {
            var slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            var slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Phần \"" + section.getTitle() + "\" trong blueprint có tổng trọng số ô câu hỏi không hợp lệ");
            }
        }
    }

    private void validateReusableSlots(List<AssessmentBlueprintSlot> slots) {
        for (var slot : slots) {
            if (slot.getSlotType() == AssessmentBlueprintSlotType.SELECTION) {
                continue;
            }
            if (slot.getFixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED phải có fixedQuestionId");
            }
        }
    }
}
