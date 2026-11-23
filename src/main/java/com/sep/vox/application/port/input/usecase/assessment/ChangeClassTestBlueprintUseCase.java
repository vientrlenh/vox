package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.question.Question;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.ChangeClassTestBlueprintCommand;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
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
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public AssessmentDto execute(ChangeClassTestBlueprintCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra trên lớp");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("Chỉ được đổi blueprint khi bài kiểm tra chưa bắt đầu");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessment.getId())) {
            throw new IllegalArgumentException("Không thể đổi blueprint khi đã có học sinh nộp bài");
        }
        if ((input.blueprintId() == null) != (input.blueprintVersionId() == null)) {
            throw new IllegalArgumentException(
                "Phải cung cấp đầy đủ blueprintId và blueprintVersionId, hoặc để trống cả hai để gỡ blueprint");
        }
        if (input.blueprintId() != null) {
            validateExistingBlueprintDuration(input, assessment);
        }

        // Đổi blueprint viết lại toàn bộ nội dung của mã đề. Bài có nhiều mã đề thì không có mã đề
        // nào là "mã đề của blueprint" cả — chặn hẳn thay vì âm thầm ghi đè mã đề đầu tiên.
        List<AssessmentPaper> papers = assessmentPaperRepository.findByAssessmentId(assessment.getId());
        if (papers.size() > 1) {
            throw new IllegalArgumentException(
                "Bài có nhiều mã đề, hãy xoá bớt chỉ còn một mã đề trước khi đổi blueprint");
        }
        AssessmentPaper paper = papers.stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Bài kiểm tra chưa có mã đề nào"));

        clearExistingPaperContent(paper, currentUserId);

        if (input.blueprintId() != null) {
            switchToExistingBlueprint(input, assessment, paper, currentUserId);
        } else {
            // Gỡ blueprint: để trống hoàn toàn, không tạo blueprint ẩn nào — bài trở về chế độ câu hỏi trực tiếp tự do.
            assessment.setBlueprintId(null);
            assessment.setBlueprintVersionId(null);
        }

        Assessment saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void validateExistingBlueprintDuration(ChangeClassTestBlueprintCommand input, Assessment assessment) {
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint"));
        if (!blueprint.getSchoolId().equals(assessment.getSchoolId())) {
            throw new IllegalArgumentException("Blueprint không thuộc trường của giáo viên");
        }
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        if (!version.getBlueprintId().equals(blueprint.getId())) {
            throw new IllegalArgumentException("Version không thuộc blueprint đã chọn");
        }
        if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ được dùng version đã PUBLISHED");
        }
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            version.getTotalTimeLimitSeconds(),
            "Phiên bản blueprint " + version.getCode()
        );
    }

    private void clearExistingPaperContent(AssessmentPaper paper, UUID currentUserId) {
        for (AssessmentPaperSection paperSection : assessmentPaperSectionRepository.findByPaperId(paper.getId())) {
            for (AssessmentPaperItem item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
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
            UUID currentUserId) {
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint"));
        if (!blueprint.getSchoolId().equals(assessment.getSchoolId())) {
            throw new IllegalArgumentException("Blueprint không thuộc trường của giáo viên");
        }
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        if (!version.getBlueprintId().equals(blueprint.getId())) {
            throw new IllegalArgumentException("Version không thuộc blueprint đã chọn");
        }
        if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ được dùng version đã PUBLISHED");
        }

        List<AssessmentBlueprintSection> sections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("Blueprint version không có section nào");
        }
        Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId = assessmentBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));
        validateVersionWeights(sections, slotsBySectionId);
        validateReusableSlots(slotsBySectionId.values().stream().flatMap(list -> list.stream()).toList());

        assessment.setBlueprintId(blueprint.getId());
        assessment.setBlueprintVersionId(version.getId());

        for (AssessmentBlueprintSection section : sections) {
            List<AssessmentBlueprintSlot> slots = slotsBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();
            AssessmentPaperSection paperSection = assessmentPaperSectionRepository.save(AssessmentPaperSection.builder()
                .paperId(paper.getId())
                .order(section.getOrder())
                .title(section.getTitle())
                .instruction(section.getInstruction())
                .sectionTimeLimitSeconds(section.getSectionTimeLimitSeconds())
                .weight(section.getSectionWeight())
                .build());
            for (AssessmentBlueprintSlot slot : slots) {
                UUID questionId = slot.getSlotType() == AssessmentBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                if (questionId != null) {
                    Question fixedQuestion = questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi cố định trong blueprint"));
                    if (fixedQuestion.getStatus() != QuestionStatus.PUBLISHED) {
                        // The fixed question was archived after the blueprint was published: leave this slot empty, the CHAIR picks another question through the picker.
                        assessmentPaperItemRepository.save(AssessmentPaperItem.builder()
                            .sectionId(paperSection.getId())
                            .paperId(paper.getId())
                            .order(slot.getOrder())
                            .weight(slot.getWeight())
                            .build());
                        continue;
                    }
                }
                AssessmentPaperItem item = AssessmentPaperItem.builder()
                    .blueprintSlotId(slot.getId())
                    .sectionId(paperSection.getId())
                    .paperId(paper.getId())
                    .questionId(questionId)
                    .order(slot.getOrder())
                    .weight(slot.getWeight())
                    .build();
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
        BigDecimal sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException(
                "Blueprint version đã chốt có tổng trọng số section không hợp lệ, không thể dùng cho bài kiểm tra");
        }
        for (AssessmentBlueprintSection section : sections) {
            List<AssessmentBlueprintSlot> slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            BigDecimal slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalArgumentException(
                    "Phần \"" + section.getTitle() + "\" trong blueprint có tổng trọng số ô câu hỏi không hợp lệ");
            }
        }
    }

    private void validateReusableSlots(List<AssessmentBlueprintSlot> slots) {
        for (AssessmentBlueprintSlot slot : slots) {
            if (slot.getSlotType() == AssessmentBlueprintSlotType.SELECTION) {
                continue;
            }
            if (slot.getFixedQuestionId() == null) {
                throw new IllegalArgumentException("Slot FIXED phải có fixedQuestionId");
            }
        }
    }
}
