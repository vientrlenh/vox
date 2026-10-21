package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.UpdateClassTestSectionCommand;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
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
public class UpdateClassTestSectionUseCase implements IUseCase<UpdateClassTestSectionCommand, AssessmentDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    public UpdateClassTestSectionUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
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
    public AssessmentDto execute(UpdateClassTestSectionCommand input) {
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

        var paperSection = assessmentPaperSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy section"));

        var now = Instant.now();

        if (input.title() != null) {
            paperSection.setTitle(input.title());
            paperSection.setUpdatedAt(now);
            paperSection.setUpdatedBy(currentUserId);
            assessmentPaperSectionRepository.save(paperSection);
        }

        if (input.instruction() != null) {
            paperSection.setInstruction(input.instruction());
            paperSection.setUpdatedAt(now);
            paperSection.setUpdatedBy(currentUserId);
            assessmentPaperSectionRepository.save(paperSection);
        }

        if (input.weight() != null) {
            paperSection.setWeight(input.weight());
            var sections = assessmentPaperSectionRepository.findByPaperId(paperSection.getPaperId()).stream()
                .map(section -> section.getId().equals(paperSection.getId()) ? paperSection : section)
                .toList();
            ClassTestSectionWeightPolicy.validateStoredWeights(sections, "Tổng trọng số section phải bằng 1.00");
            paperSection.setUpdatedAt(now);
            paperSection.setUpdatedBy(currentUserId);
            assessmentPaperSectionRepository.save(paperSection);
        }

        if (input.questions() != null) {
            if (input.questions().isEmpty()) {
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
            updateSectionQuestions(paperSection, input.questions(), assessment.getId(), currentUserId, now);
        }

        assessment.setUpdatedAt(now);
        assessment.setUpdatedBy(currentUserId);
        var saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDto.toDto(saved);
    }

    private void requireNoAttachedBlueprint(Assessment assessment) {
        if (assessment.getBlueprintId() != null) {
            throw new IllegalStateException(
                "Bài đang dùng blueprint dùng chung, không thể sửa câu hỏi trực tiếp — dùng \"Đổi blueprint khác\" ở tab Blueprint để thay đổi cấu trúc");
        }
    }

    private void updateSectionQuestions(
            AssessmentPaperSection paperSection,
            List<ClassTestQuestionCommand> questions,
            UUID assessmentId,
            UUID currentUserId,
            Instant now) {
        var existingItems = assessmentPaperItemRepository.findBySectionId(paperSection.getId()).stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .toList();
        for (var item : existingItems) {
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
        paperSection.setUpdatedAt(now);
        paperSection.setUpdatedBy(currentUserId);
        assessmentPaperSectionRepository.save(paperSection);
    }
}
