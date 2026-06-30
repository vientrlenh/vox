package com.sep.vox.application.port.input.usecase.exampaper;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamPaperCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperItem;
import com.sep.vox.domain.model.exam.ExamPaperSection;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class CreateExamPaperUseCase implements IUseCase<CreateExamPaperCommand, ExamPaperDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final UserContextPort userContextPort;

    public CreateExamPaperUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperDto execute(CreateExamPaperCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        if (exam.getKind() != ExamKind.CENTRALIZED) {
            throw new ForbiddenException("Bài kiểm tra trên lớp không tạo đề qua endpoint này");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.AUTHOR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (exam.getBlueprintId() == null) {
            throw new IllegalStateException("Bài kiểm tra chưa gắn blueprint");
        }

        var version = examBlueprintVersionRepository
            .findByBlueprintIdAndStatus(exam.getBlueprintId(), ExamBlueprintVersionStatus.PUBLISHED)
            .stream()
            .max(Comparator.comparingInt(v -> v.getVersion()))
            .orElseThrow(() -> new IllegalStateException("Blueprint chưa có version nào được publish"));

        var now = OffsetDateTime.now();
        var variant = examPaperRepository.nextVariant(exam.getId());
        var paper = examPaperRepository.save(new ExamPaper(
            exam.getId(),
            exam.getCode() + "-P" + variant,
            variant,
            ExamPaperStatus.DRAFT,
            now,
            now,
            currentUserId,
            currentUserId
        ));

        List<ExamBlueprintSection> sections = examBlueprintSectionRepository
            .findByBlueprintVersionId(version.getId())
            .stream()
            .sorted(Comparator.comparingInt(s -> s.getOrder()))
            .toList();

        for (var section : sections) {
            var savedSection = examPaperSectionRepository.save(new ExamPaperSection(
                paper.getId(),
                section.getOrder(),
                section.getTitle(),
                section.getInstruction(),
                section.getSectionTimeLimitSeconds(),
                now,
                now,
                currentUserId,
                currentUserId
            ));

            var slots = examBlueprintSlotRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(s -> s.getOrder()))
                .toList();

            for (var slot : slots) {
                var questionId = slot.getSlotType() == ExamBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
                if (questionId != null) {
                    var fixedQuestion = questionRepository.findById(questionId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi cố định trong slot"));
                    if (fixedQuestion.getStatus() != QuestionStatus.PUBLISHED) {
                        throw new IllegalStateException(
                            "Câu hỏi " + fixedQuestion.getCode() + " trong khuôn chưa PUBLISHED, không thể sinh đề thi");
                    }
                }
                examPaperItemRepository.save(new ExamPaperItem(
                    slot.getId(),
                    savedSection.getId(),
                    paper.getId(),
                    questionId,
                    slot.getOrder(),
                    slot.getWeight()
                ));
                if (questionId != null) {
                    examQuestionSecureLockService.lockQuestionForExam(
                        questionId,
                        exam.getId(),
                        ExamSecurePoolReleaseMode.MANUAL,
                        currentUserId
                    );
                }
            }
        }

        return ExamPaperDtoMapper.toDto(paper);
    }
}
