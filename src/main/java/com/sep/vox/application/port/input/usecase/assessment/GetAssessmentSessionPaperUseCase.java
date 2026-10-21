package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionPaperQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.StudentAssessmentPaperQuestionResponse;
import com.sep.vox.application.response.input.assessment.StudentAssessmentPaperResponse;
import com.sep.vox.application.response.input.assessment.StudentQuestionResponse;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

@Service
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

    public GetAssessmentSessionPaperUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            UserContextPort userContextPort,
            QuestionAssetRepository questionAssetRepository) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.userContextPort = userContextPort;
        this.questionAssetRepository = questionAssetRepository;
    }

    @Override
    public StudentAssessmentPaperResponse execute(ViewAssessmentSessionPaperQuery input) {
        var session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        var candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi"));
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        if (!candidate.getStudentId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không được phép xem đề thi của phiên này");
        }

        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var paper = assessmentPaperRepository.findById(session.getPaperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));

        var sections = assessmentPaperSectionRepository.findByPaperId(session.getPaperId());
        var sectionById = new HashMap<>(sections.stream()
            .collect(java.util.stream.Collectors.toMap(section -> section.getId(), section -> section)));
        var paperQuestions = sections.stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .flatMap(section -> assessmentPaperItemRepository.findBySectionId(section.getId()).stream()
                .sorted(Comparator.comparingInt(item -> item.getOrder())))
            .map(item -> {
                var question = questionRepository.findById(item.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi trong đề thi"));
                var section = sectionById.get(item.getSectionId());
                var asset = questionAssetRepository.findByQuestionId(question.getId()).stream()
                    .findFirst()
                    .map(QuestionAssetDto::toDto)
                    .orElse(null);

                return new StudentAssessmentPaperQuestionResponse(
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
            : assessmentScheduleRepository.findById(candidate.getScheduleId()).orElse(null);
        var durationSeconds = paper.getTimeDurationSeconds() == null
            ? estimateDurationSeconds(session.getPaperId())
            : paper.getTimeDurationSeconds();
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
            schedule == null
                ? (assessment.getCloseAt() == null ? null : assessment.getCloseAt().toString())
                : (schedule.getEndDate() == null ? null : schedule.getEndDate().toString()),
            session.getStartedAt() == null ? null : session.getStartedAt().toString(),
            session.getRemainingSeconds(),
            paperQuestions
        );
    }

    private static int orderOf(java.util.Map<java.util.UUID, com.sep.vox.domain.model.assessment.AssessmentPaperSection> sectionsById,
            java.util.UUID sectionId,
            int itemOrder) {
        var section = sectionsById.get(sectionId);
        var sectionOrder = section == null ? 0 : section.getOrder();
        return sectionOrder * 1000 + itemOrder;
    }

    /**
     * Chỉ dùng khi mã đề chưa có {@code timeDurationSeconds} -- hiếm, vì mọi đường tạo mã đề đều gọi
     * {@code RecalculateAssessmentTimeDurationService} ngay sau đó. Tính lại từ domain object chứ không từ
     * response DTO để đi đúng một công thức với mọi nơi khác ({@link PaperTimeCalculator}); nếu tự
     * cộng ở đây thì thí sinh sẽ nhận đồng hồ thiếu đúng phần thời lượng phát AUDIO/VIDEO, mà lại
     * không có gì để đối chiếu nên sai hoàn toàn âm thầm.
     */
    private int estimateDurationSeconds(UUID paperId) {
        var questions = assessmentPaperItemRepository.findByPaperId(paperId).stream()
            .map(item -> item.getQuestionId())
            .filter(Objects::nonNull)
            .map(questionRepository::findById)
            .flatMap(o -> o.stream())
            .toList();
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(q -> q.getId()).distinct().toList()));
        return PaperTimeCalculator.breakdownOf(questions, assetByQuestionId).totalSeconds();
    }

    private static int durationMinutesOf(int durationSeconds) {
        return Math.max(1, (int) Math.ceil(durationSeconds / 60.0));
    }
}
