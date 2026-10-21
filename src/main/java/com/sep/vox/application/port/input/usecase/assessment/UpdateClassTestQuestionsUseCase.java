package com.sep.vox.application.port.input.usecase.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.UpdateClassTestQuestionsCommand;
import com.sep.vox.application.port.input.service.ClassTestPaperResolver;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
public class UpdateClassTestQuestionsUseCase implements IUseCase<UpdateClassTestQuestionsCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final ClassTestPaperResolver classTestPaperResolver;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    public UpdateClassTestQuestionsUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            ClassTestPaperResolver classTestPaperResolver,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService,
            RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.classTestPaperResolver = classTestPaperResolver;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.assessmentQuestionSecureLockService = assessmentQuestionSecureLockService;
        this.assessmentTimeQuotaGuardService = assessmentTimeQuotaGuardService;
        this.recalculateAssessmentTimeDurationService = recalculateAssessmentTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentDto execute(UpdateClassTestQuestionsCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
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
        if (input.sections() == null || input.sections().isEmpty()) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có ít nhất 1 section");
        }
        var seenQuestionIds = new java.util.HashSet<UUID>();
        for (var section : input.sections()) {
            if (section.questions() == null || section.questions().isEmpty()) {
                throw new IllegalStateException("Mỗi section phải có ít nhất 1 câu hỏi");
            }
            for (var questionCommand : section.questions()) {
                if (!seenQuestionIds.add(questionCommand.questionId())) {
                    throw new IllegalStateException("Một câu hỏi không thể xuất hiện nhiều lần trong cùng 1 bài kiểm tra");
                }
            }
        }

        var candidateQuestions = new ArrayList<Question>();
        for (var section : input.sections()) {
            for (var questionCommand : section.questions()) {
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
                candidateQuestions.add(question);
            }
        }
        // Chiếu TRƯỚC khi ghi nên không dùng lại được RecalculateAssessmentTimeDurationService. Dùng
        // totalSeconds (đã gồm thời lượng phát AUDIO/VIDEO) vì đây là thước đo ĐỘ DÀI so với gói.
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(candidateQuestions.stream().map(q -> q.getId()).distinct().toList()));
        var candidateDurationSeconds = PaperTimeCalculator
            .breakdownOf(candidateQuestions, assetByQuestionId).totalSeconds();
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            candidateDurationSeconds,
            "Mã đề trên lớp"
        );

        var paper = classTestPaperResolver.resolve(assessment.getId(), input.paperId());
        var existingPaperSections = assessmentPaperSectionRepository.findByPaperId(paper.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();

        var now = Instant.now();
        var commonCount = Math.min(existingPaperSections.size(), input.sections().size());
        var sectionWeights = ClassTestSectionWeightPolicy.resolveRequestedWeights(input.sections());

        for (int i = 0; i < commonCount; i++) {
            var existingPaperSection = existingPaperSections.get(i);
            var sectionCommand = input.sections().get(i);

            existingPaperSection.setTitle(sectionCommand.title());
            existingPaperSection.setInstruction(sectionCommand.instruction());
            existingPaperSection.setWeight(sectionWeights.get(i));
            existingPaperSection.setUpdatedAt(now);
            existingPaperSection.setUpdatedBy(currentUserId);
            assessmentPaperSectionRepository.save(existingPaperSection);

            updateSectionQuestions(existingPaperSection, sectionCommand.questions(), assessment.getId(), currentUserId, now);
        }

        for (int i = input.sections().size(); i < existingPaperSections.size(); i++) {
            deleteSection(existingPaperSections.get(i));
        }

        for (int i = existingPaperSections.size(); i < input.sections().size(); i++) {
            createNewSection(paper, input.sections().get(i), sectionWeights.get(i), i + 1, assessment.getId(), currentUserId, now);
        }

        assessment.setUpdatedAt(now);
        assessment.setUpdatedBy(currentUserId);
        var saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDto.toDto(saved);
    }

    private void updateSectionQuestions(
            AssessmentPaperSection paperSection,
            List<ClassTestQuestionCommand> questions,
            UUID assessmentId,
            UUID currentUserId,
            Instant now) {
        for (var item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
            assessmentPaperItemRepository.deleteById(item.getId());
        }

        var weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            var questionId = questions.get(i).questionId();
            assessmentPaperItemRepository.save(new AssessmentPaperItem(
                null,
                paperSection.getId(),
                paperSection.getPaperId(),
                questionId,
                i + 1,
                weights.get(i)
            ));
            assessmentQuestionSecureLockService.lockQuestionForAssessment(
                questionId, assessmentId, AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private void deleteSection(AssessmentPaperSection paperSection) {
        for (var item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
            assessmentPaperItemRepository.deleteById(item.getId());
        }
        assessmentPaperSectionRepository.deleteById(paperSection.getId());
    }

    private void createNewSection(
            AssessmentPaper paper,
            ClassTestSectionCommand sectionCommand,
            BigDecimal sectionWeight,
            int order,
            UUID assessmentId,
            UUID currentUserId,
            Instant now) {
        var paperSection = assessmentPaperSectionRepository.save(new AssessmentPaperSection(
            paper.getId(),
            order,
            sectionCommand.title(),
            sectionCommand.instruction(),
            null,
            sectionWeight,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        var questions = sectionCommand.questions();
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
                questionId, assessmentId, AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private void requireNoAttachedBlueprint(Assessment assessment) {
        if (assessment.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể sửa câu hỏi trực tiếp — dùng \"Đổi blueprint khác\" ở tab Blueprint để thay đổi cấu trúc");
        }
    }

}
