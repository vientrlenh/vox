package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;

/** Phần ghi DB (đọc đề đã giữ chỗ + tạo session + đổi trạng thái đề) của
 * StartPracticeSessionUseCase -- tách riêng để @Transactional không bọc luôn
 * agentAvailabilityClient.requireReady() (health-check ra ngoài, timeout tới 12s). */
@Service
public class StartPracticeSessionPersistenceService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticePaperRepository practicePaperRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;

    public StartPracticeSessionPersistenceService(
            PracticeSessionRepository practiceSessionRepository,
            PracticePaperRepository practicePaperRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practicePaperRepository = practicePaperRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
    }

    @Transactional
    public PracticeSession persist(UUID studentId, UUID paperId) {
        var now = Instant.now();
        var paper = practicePaperRepository
            .findReservedPaper(paperId, studentId, now)
            .orElseThrow(() -> new NotFoundException(
                "Đề luyện không tồn tại hoặc đã hết thời gian giữ chỗ."
            ));
        var policy = currentPolicy(studentId);
        var sessionId = UUID.randomUUID();
        practiceSessionRepository.save(new com.sep.vox.domain.model.personalization.PracticeSession(
            sessionId,
            studentId,
            paper.getId(),
            policy.rubricVersionId(),
            policy.targetFrameworkBandId(),
            paper.getPracticeTopicId(),
            "[]",
            paper.getOrigin(),
            offerEvidenceJson(paper.getOfferedTopicIdsJson(), paper.getPreviousOfferedTopicIdsJson()),
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
