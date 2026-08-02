package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.port.input.command.StartPracticeSessionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.infrastructure.service.PracticeAgentAvailabilityClient;

@Service
public class StartPracticeSessionUseCase implements IUseCase<StartPracticeSessionCommand, PracticeSession> {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticePaperRepository practicePaperRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeAgentAvailabilityClient agentAvailabilityClient;
    private final UserContextPort userContextPort;

    public StartPracticeSessionUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticePaperRepository practicePaperRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeAgentAvailabilityClient agentAvailabilityClient,
            UserContextPort userContextPort) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practicePaperRepository = practicePaperRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.agentAvailabilityClient = agentAvailabilityClient;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PracticeSession execute(StartPracticeSessionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var now = OffsetDateTime.now();
        var paper = practicePaperRepository
            .findReservedPaper(input.paperId(), studentId, now)
            .orElseThrow(() -> new NotFoundException(
                "Đề luyện không tồn tại hoặc đã hết thời gian giữ chỗ."
            ));
        agentAvailabilityClient.requireReady();
        var policy = currentPolicy(studentId);
        var sessionId = UUID.randomUUID();
        practiceSessionRepository.save(new com.sep.vox.domain.model.personalization.PracticeSession(
            sessionId,
            studentId,
            paper.id(),
            policy.rubricVersionId(),
            policy.targetFrameworkBandId(),
            paper.practiceTopicId(),
            "[]",
            paper.origin(),
            offerEvidenceJson(paper.offeredTopicIdsJson(), paper.previousOfferedTopicIdsJson()),
            null,
            now,
            null,
            now,
            0,
            "IN_PROGRESS",
            null,
            0,
            0
        ));
        practicePaperRepository.save(paper.withStatus("STARTED"));
        return PracticeSessionResponseMapper.toResponse(
            SessionRowMapper.toDto(
                practiceSessionQueryRepository.findSessionRow(sessionId, studentId).orElse(null)
            )
        );
    }

    private AssessmentPolicyRepository.CurrentPolicy currentPolicy(UUID studentId) {
        var rows = assessmentPolicyRepository.findCurrentPolicyForStudent(studentId);
        if (rows.isEmpty()) {
            throw new NotFoundException("Không tìm thấy chính sách chấm đang hiệu lực.");
        }
        return rows.get(0);
    }

    private String offerEvidenceJson(String current, String previous) {
        var currentJson = current == null || current.isBlank() ? "[]" : current;
        var previousJson = previous == null || previous.isBlank() ? "[]" : previous;
        return "{\"current\":%s,\"previous\":%s}".formatted(currentJson, previousJson);
    }
}
