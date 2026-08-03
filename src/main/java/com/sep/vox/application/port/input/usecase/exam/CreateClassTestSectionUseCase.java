package com.sep.vox.application.port.input.usecase.exam;

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
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateClassTestSectionCommand;
import com.sep.vox.application.port.input.service.ClassTestPaperResolver;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class CreateClassTestSectionUseCase implements IUseCase<CreateClassTestSectionCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ClassTestPaperResolver classTestPaperResolver;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public CreateClassTestSectionUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ClassTestPaperResolver classTestPaperResolver,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.classTestPaperResolver = classTestPaperResolver;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.recalculateExamTimeDurationService = recalculateExamTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(CreateClassTestSectionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (exam.getKind() != ExamKind.CLASS_TEST) {
            throw new ForbiddenException("Chỉ áp dụng cho bài kiểm tra trên lớp");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ được sửa khi bài kiểm tra chưa bắt đầu");
        }
        if (examRepository.existsSubmittedSessionByExamId(exam.getId())) {
            throw new IllegalStateException("Không thể sửa câu hỏi khi đã có học sinh nộp bài");
        }
        requireNoAttachedBlueprint(exam);
        if (input.questions() == null || input.questions().isEmpty()) {
            throw new IllegalStateException("Section phải có ít nhất 1 câu hỏi");
        }

        for (var questionCommand : input.questions()) {
            var question = questionRepository.findAccessibleById(questionCommand.questionId(), currentUserId, exam.getSchoolId(), false, false)
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

        var paper = classTestPaperResolver.resolve(exam.getId(), input.paperId());
        var order = examPaperSectionRepository.findByPaperId(paper.getId()).size() + 1;

        var now = Instant.now();
        var paperSection = examPaperSectionRepository.save(new ExamPaperSection(
            paper.getId(), order, input.title(), input.instruction(), null, input.weight(), now, now, currentUserId, currentUserId
        ));

        var questions = input.questions();
        var weights = ClassTestSectionWeightPolicy.resolveQuestionWeights(questions);
        for (int i = 0; i < questions.size(); i++) {
            var questionId = questions.get(i).questionId();
            examPaperItemRepository.save(new ExamPaperItem(
                null,
                paperSection.getId(),
                paper.getId(),
                questionId,
                i + 1,
                weights.get(i)
            ));
            examQuestionSecureLockService.lockQuestionForExam(
                questionId, exam.getId(), ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
        if (input.weight() == null) {
            rebalanceSectionWeights(paper.getId(), now, currentUserId);
        } else {
            var sections = examPaperSectionRepository.findByPaperId(paper.getId()).stream()
                .sorted(Comparator.comparingInt(section -> section.getOrder()))
                .toList();
            ClassTestSectionWeightPolicy.validateStoredWeights(sections, "Tổng trọng số section phải bằng 1.00");
        }

        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        var saved = examRepository.save(exam);
        recalculateExamTimeDurationService.recalculate(exam.getId());
        return ExamDtoMapper.toDto(saved);
    }

    private void requireNoAttachedBlueprint(Exam exam) {
        if (exam.getBlueprintId() != null) {
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
        var sections = examPaperSectionRepository.findByPaperId(paperId).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        var weights = distributeEqualWeights(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            section.setWeight(weights.get(i));
            section.setUpdatedAt(now);
            section.setUpdatedBy(currentUserId);
            examPaperSectionRepository.save(section);
        }
    }
}
