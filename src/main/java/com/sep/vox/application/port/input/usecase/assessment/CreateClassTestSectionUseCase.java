package com.sep.vox.application.port.input.usecase.assessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateClassTestSectionCommand;
import com.sep.vox.application.port.input.service.ClassTestPaperResolver;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
public class CreateClassTestSectionUseCase implements IUseCase<CreateClassTestSectionCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final ClassTestPaperResolver classTestPaperResolver;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    public CreateClassTestSectionUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            ClassTestPaperResolver classTestPaperResolver,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.classTestPaperResolver = classTestPaperResolver;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.assessmentQuestionSecureLockService = assessmentQuestionSecureLockService;
        this.recalculateAssessmentTimeDurationService = recalculateAssessmentTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentDto execute(CreateClassTestSectionCommand input) {
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
            throw new IllegalStateException("Chỉ được sửa khi bài kiểm tra chưa bắt đầu");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessment.getId())) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi đã có học sinh nộp bài");
        }
        requireNoAttachedBlueprint(assessment);
        if (input.questions() == null || input.questions().isEmpty()) {
            throw new IllegalStateException("Section phải có ít nhất 1 câu hỏi");
        }

        for (var questionCommand : input.questions()) {
            var question = questionRepository.findAccessibleById(questionCommand.questionId(), currentUserId, assessment.getSchoolId(), false, false)
                .orElseThrow(() -> new ForbiddenException("Không có quyền dùng câu hỏi " + questionCommand.questionId()));
            boolean isOwner = currentUserId.equals(question.getCreatedBy());
            boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
            if (!isOwner && !isSchoolShared) {
                var collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
                if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                    throw new ForbiddenException("Quyền READ_ONLY không được phép dùng câu hỏi trong bài kiểm tra");
                }
            }
        }

        var paper = classTestPaperResolver.resolve(assessment.getId(), input.paperId());
        var order = assessmentPaperSectionRepository.findByPaperId(paper.getId()).size() + 1;

        var now = Instant.now();
        var paperSection = assessmentPaperSectionRepository.save(new AssessmentPaperSection(
            paper.getId(), order, input.title(), input.instruction(), null, input.weight(), now, now, currentUserId, currentUserId
        ));

        var questions = input.questions();
        var weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            var questionId = questions.get(i).questionId();
            assessmentPaperItemRepository.save(new AssessmentPaperItem(
                null,
                paperSection.getId(),
                paper.getId(),
                questionId,
                i + 1,
                weights.get(i)
            ));
            assessmentQuestionSecureLockService.lockQuestionForAssessment(
                questionId, assessment.getId(), AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
        if (input.weight() == null) {
            rebalanceSectionWeights(paper.getId(), now, currentUserId);
        } else {
            var sections = assessmentPaperSectionRepository.findByPaperId(paper.getId()).stream()
                .sorted(Comparator.comparingInt(section -> section.getOrder()))
                .toList();
            ClassTestSectionWeightPolicy.validateStoredWeights(sections, "Tổng trọng số section phải bằng 1.00");
        }

        assessment.setUpdatedAt(now);
        assessment.setUpdatedBy(currentUserId);
        var saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void requireNoAttachedBlueprint(Assessment assessment) {
        if (assessment.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể sửa câu hỏi trực tiếp — dùng \"Đổi blueprint khác\" ở tab Blueprint để thay đổi cấu trúc");
        }
    }

    private List<BigDecimal> distributeEqualWeights(int count) {
        var weights = new ArrayList<BigDecimal>();
        var perItem = BigDecimal.ONE.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        var runningSum = BigDecimal.ZERO;
        for (int i = 0; i < count - 1; i++) {
            weights.add(perItem);
            runningSum = runningSum.add(perItem);
        }
        weights.add(BigDecimal.ONE.subtract(runningSum));
        return weights;
    }

    private void rebalanceSectionWeights(UUID paperId, Instant now, UUID currentUserId) {
        var sections = assessmentPaperSectionRepository.findByPaperId(paperId).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        var weights = distributeEqualWeights(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            section.setWeight(weights.get(i));
            section.setUpdatedAt(now);
            section.setUpdatedBy(currentUserId);
            assessmentPaperSectionRepository.save(section);
        }
    }
}
