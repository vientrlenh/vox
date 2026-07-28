package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamSessionPaperQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.exam.StudentExamPaperQuestionResponse;
import com.sep.vox.application.response.input.exam.StudentExamPaperResponse;
import com.sep.vox.application.response.input.exam.StudentQuestionResponse;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class GetExamSessionPaperUseCase implements IUseCase<ViewExamSessionPaperQuery, StudentExamPaperResponse> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final UserContextPort userContextPort;
    private final QuestionAssetRepository questionAssetRepository;

    public GetExamSessionPaperUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            ExamScheduleRepository examScheduleRepository,
            UserContextPort userContextPort,
            QuestionAssetRepository questionAssetRepository) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.userContextPort = userContextPort;
        this.questionAssetRepository = questionAssetRepository;
    }

    @Override
    public StudentExamPaperResponse execute(ViewExamSessionPaperQuery input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        if (!candidate.getStudentId().equals(userContextPort.getCurrentAuthenticatedUserId())) {
            throw new ForbiddenException("Bạn không được phép xem đề thi của phiên này");
        }

        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var paper = examPaperRepository.findById(session.getPaperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));

        var sections = examPaperSectionRepository.findByPaperId(session.getPaperId());
        var sectionById = new HashMap<>(sections.stream()
            .collect(java.util.stream.Collectors.toMap(section -> section.getId(), section -> section)));
        var paperQuestions = sections.stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .flatMap(section -> examPaperItemRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(item -> item.getOrder())))
            .map(item -> {
                var question = questionRepository.findById(item.getQuestionId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi trong đề thi"));
                var section = sectionById.get(item.getSectionId());
                var asset = questionAssetRepository.findByQuestionId(question.getId()).stream()
                    .findFirst()
                    .map(QuestionAssetDtoMapper::toDto)
                    .orElse(null);

                return new StudentExamPaperQuestionResponse(
                    item.getId(),
                    orderOf(sectionById, item.getSectionId(), item.getOrder()),
                    item.getSectionId(),
                    section == null ? null : section.getTitle(),
                    section == null ? null : section.getInstruction(),
                    new StudentQuestionResponse(
                        question.getId(),
                        question.getCode(),
                        question.getInstructionText(),
                        question.getQuestionText(),
                        question.getPromptText(),
                        question.getPreparationText(),
                        question.getPreparationTimeSeconds(),
                        question.getMinResponseSeconds(),
                        question.getMaxResponseSeconds(),
                        question.getType() == null ? null : question.getType().name(),
                        "",
                        asset
                    ),
                    asset,
                    questionEvaluationGuideRepository.findByQuestionId(question.getId()).map(guide -> new com.sep.vox.domain.dto.QuestionEvaluationGuideDto(
                        guide.getId(),
                        guide.getQuestionId(),
                        guide.getExpectedContent(),
                        guide.getKeyPoints(),
                        guide.getAcceptableResponses(),
                        guide.getOffTopicExamples(),
                        guide.getScoringHints(),
                        guide.getCommonMistakes()
                    )).orElse(null)
                );
            })
            .toList();

        var schedule = candidate.getScheduleId() == null
            ? null
            : examScheduleRepository.findById(candidate.getScheduleId()).orElse(null);
        var calculatedDurationSeconds = estimateDurationSeconds(paperQuestions);
        var durationSeconds = paper.getTimeDurationSeconds() == null
            ? calculatedDurationSeconds
            : paper.getTimeDurationSeconds();
        return new StudentExamPaperResponse(
            exam.getId(),
            session.getPaperId(),
            exam.getName(),
            StudentExamViewSupport.subjectOf(exam),
            exam.getDescription(),
            durationSeconds,
            durationMinutesOf(durationSeconds),
            StudentExamViewSupport.examDateOf(schedule, exam.getOpenAt()),
            StudentExamViewSupport.statusOf(schedule, OffsetDateTime.now()),
            schedule == null
                ? (exam.getCloseAt() == null ? null : exam.getCloseAt().toString())
                : (schedule.getEndDate() == null ? null : schedule.getEndDate().toString()),
            session.getStartedAt() == null ? null : session.getStartedAt().toString(),
            session.getRemainingSeconds(),
            paperQuestions
        );
    }

    private static int orderOf(java.util.Map<java.util.UUID, com.sep.vox.domain.model.exam.ExamPaperSection> sectionsById,
            java.util.UUID sectionId,
            int itemOrder) {
        var section = sectionsById.get(sectionId);
        var sectionOrder = section == null ? 0 : section.getOrder();
        return sectionOrder * 1000 + itemOrder;
    }

    private static int estimateDurationSeconds(java.util.List<StudentExamPaperQuestionResponse> paperQuestions) {
        return paperQuestions.stream()
            .map(r -> r.question())
            .mapToInt(question -> question.preparationTimeSeconds() + question.maxResponseSeconds())
            .sum();
    }

    private static int durationMinutesOf(int durationSeconds) {
        return Math.max(1, (int) Math.ceil(durationSeconds / 60.0));
    }
}
