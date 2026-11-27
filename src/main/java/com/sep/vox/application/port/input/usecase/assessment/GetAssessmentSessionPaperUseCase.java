package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.assessment.ViewAssessmentSessionPaperQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.StudentAssessmentPaperQuestionResponse;
import com.sep.vox.application.response.input.assessment.StudentAssessmentPaperResponse;
import com.sep.vox.application.response.input.assessment.StudentQuestionResponse;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
@RequiredArgsConstructor
public class GetAssessmentSessionPaperUseCase implements IUseCase<ViewAssessmentSessionPaperQuery, StudentAssessmentPaperResponse> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final UserContextPort userContextPort;
    private final QuestionAssetRepository questionAssetRepository;

    @Override
    public StudentAssessmentPaperResponse execute(ViewAssessmentSessionPaperQuery input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi"));
        if (!candidate.getStudentId().equals(userContextPort.getCurrentAuthenticatedUserId())) {
            throw new ForbiddenException("Bạn không được phép xem đề thi của phiên này");
        }

        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        AssessmentPaper paper = assessmentPaperRepository.findById(session.getPaperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));

        List<StudentAssessmentPaperQuestionResponse> paperQuestions = loadPaperQuestions(session.getPaperId());
        return toPaperResponse(assessment, paper, session, candidate, paperQuestions);
    }

    private List<StudentAssessmentPaperQuestionResponse> loadPaperQuestions(UUID paperId) {
        List<AssessmentPaperSection> sections = assessmentPaperSectionRepository.findByPaperId(paperId);
        Map<UUID, AssessmentPaperSection> sectionById = new HashMap<>(sections.stream()
            .collect(java.util.stream.Collectors.toMap(section -> section.getId(), section -> section)));
        return sections.stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .flatMap(section -> assessmentPaperItemRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(item -> item.getOrder())))
            .map(item -> toPaperQuestion(item, sectionById))
            .toList();
    }

    private StudentAssessmentPaperQuestionResponse toPaperQuestion(
            AssessmentPaperItem item,
            Map<UUID, AssessmentPaperSection> sectionById) {
        Question question = questionRepository.findById(item.getQuestionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi trong đề thi"));
        AssessmentPaperSection section = sectionById.get(item.getSectionId());
        QuestionAssetDto asset = questionAssetRepository.findByQuestionId(question.getId()).stream()
            .findFirst()
            .map(QuestionAssetDtoMapper::toDto)
            .orElse(null);

        return new StudentAssessmentPaperQuestionResponse(
            item.getId(),
            orderOf(sectionById, item.getSectionId(), item.getOrder()),
            item.getSectionId(),
            section == null ? null : section.getTitle(),
            section == null ? null : section.getInstruction(),
            toStudentQuestion(question, asset),
            asset,
            evaluationGuideOf(question)
        );
    }

    private StudentQuestionResponse toStudentQuestion(Question question, QuestionAssetDto asset) {
        return new StudentQuestionResponse(
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
        );
    }

    private QuestionEvaluationGuideDto evaluationGuideOf(Question question) {
        return questionEvaluationGuideRepository.findByQuestionId(question.getId()).map(guide -> new QuestionEvaluationGuideDto(
            guide.getId(),
            guide.getQuestionId(),
            guide.getExpectedContent(),
            guide.getKeyPoints(),
            guide.getAcceptableResponses(),
            guide.getOffTopicExamples(),
            guide.getScoringHints(),
            guide.getCommonMistakes()
        )).orElse(null);
    }

    private StudentAssessmentPaperResponse toPaperResponse(
            Assessment assessment,
            AssessmentPaper paper,
            AssessmentSession session,
            AssessmentCandidate candidate,
            List<StudentAssessmentPaperQuestionResponse> paperQuestions) {
        AssessmentSchedule schedule = scheduleOf(candidate);
        int durationSeconds = durationSecondsOf(paper, session);
        return new StudentAssessmentPaperResponse(
            assessment.getId(),
            session.getPaperId(),
            assessment.getName(),
            StudentAssessmentViewSupport.subjectOf(assessment),
            assessment.getDescription(),
            durationSeconds,
            durationMinutesOf(durationSeconds),
            StudentAssessmentViewSupport.assessmentDateOf(schedule, assessment.getOpenAt()),
            StudentAssessmentViewSupport.statusOf(schedule, Instant.now()),
            closeTimeOf(schedule, assessment),
            session.getStartedAt() == null ? null : session.getStartedAt().toString(),
            session.getRemainingSeconds(),
            paperQuestions
        );
    }

    private AssessmentSchedule scheduleOf(AssessmentCandidate candidate) {
        return candidate.getScheduleId() == null
            ? null
            : assessmentScheduleRepository.findById(candidate.getScheduleId()).orElse(null);
    }

    private int durationSecondsOf(AssessmentPaper paper, AssessmentSession session) {
        return paper.getTimeDurationSeconds() == null
            ? estimateDurationSeconds(session.getPaperId())
            : paper.getTimeDurationSeconds();
    }

    private static String closeTimeOf(AssessmentSchedule schedule, Assessment assessment) {
        Instant closeTime = schedule == null ? assessment.getCloseAt() : schedule.getEndDate();
        return closeTime == null ? null : closeTime.toString();
    }

    private static int orderOf(java.util.Map<java.util.UUID, com.sep.vox.domain.model.assessment.AssessmentPaperSection> sectionsById,
            java.util.UUID sectionId,
            int itemOrder) {
        AssessmentPaperSection section = sectionsById.get(sectionId);
        int sectionOrder = section == null ? 0 : section.getOrder();
        return sectionOrder * 1000 + itemOrder;
    }

    /**
     * Only used when the paper variant has no {@code timeDurationSeconds} yet -- rare, because every path that creates a paper variant calls
     * {@code RecalculateAssessmentTimeDurationService} right afterwards. Recomputed from the domain object and not from the
     * response DTO so that it follows exactly one formula with every other place ({@link PaperTimeCalculator}); if it were summed
     * by hand here the candidate would get a clock that lacks exactly the AUDIO/VIDEO playback duration, and with
     * nothing to cross-check against it would be wrong completely silently.
     */
    private int estimateDurationSeconds(UUID paperId) {
        List<Question> questions = assessmentPaperItemRepository.findByPaperId(paperId).stream()
            .map(item -> item.getQuestionId())
            .filter(Objects::nonNull)
            .map(questionRepository::findById)
            .flatMap(o -> o.stream())
            .toList();
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(q -> q.getId()).distinct().toList()));
        return PaperTimeCalculator.breakdownOf(questions, assetByQuestionId).totalSeconds();
    }

    private static int durationMinutesOf(int durationSeconds) {
        return Math.max(1, (int) Math.ceil(durationSeconds / 60.0));
    }
}
